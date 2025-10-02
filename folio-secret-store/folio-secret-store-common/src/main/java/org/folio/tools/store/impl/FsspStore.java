package org.folio.tools.store.impl;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.wrapIfMissing;
import static org.apache.commons.lang3.Strings.CS;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.entity.ContentType.APPLICATION_JSON;
import static org.folio.tools.store.impl.Validation.validateKey;
import static org.folio.tools.store.impl.Validation.validateValue;
import static org.folio.tools.store.utils.TlsUtils.IS_HOSTNAME_VERIFICATION_DISABLED;
import static org.folio.tools.store.utils.TlsUtils.buildSslContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.folio.tools.store.exception.SecureStoreServiceException;
import org.folio.tools.store.properties.FsspConfigProperties;
import org.folio.tools.store.utils.TlsProperties;
import org.folio.tools.store.utils.TlsProperties.Store;

@Log4j2
public class FsspStore implements SecureStore {

  public static final String TYPE = "Fssp";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final HttpClientBuilder httpClientBuilder;
  private final String secretUri;

  public FsspStore(FsspConfigProperties properties) {
    requireNonNull(properties, "Properties cannot be null");
    try {
      this.httpClientBuilder = HttpClientBuilderFactory.create(properties);
      this.secretUri = buildUri(properties);
    } catch (Exception e) {
      log.error("Failed to initialize: ", e);
      throw new IllegalStateException(format("Cannot initialize Secure Store Proxy: message = %s", e.getMessage()), e);
    }
  }

  public FsspStore(Properties properties) {
    this(FsspConfigProperties.from(properties));
  }

  @Override
  public String get(String key) {
    validateKey(key);

    try (var client = httpClientBuilder.build()) {
      var request = new HttpGet(secretUri + key);
      request.setHeader(ACCEPT, APPLICATION_JSON.getMimeType());

      log.debug("Sending request to get secret: key = {}", key);
      return client.execute(request, handleGet(key));
    } catch (IOException e) {
      throw serviceException("Failed to get secret: key = " + key, e);
    }
  }

  @Override
  public void set(String key, String value) {
    validateKey(key);
    validateValue(value);

    try (var client = httpClientBuilder.build()) {
      var request = new HttpPut(secretUri + key);
      request.setEntity(serializeSecureStoreEntry(SecureStoreEntry.of(key, value)));

      log.debug("Sending request to save secret: key = {}", key);
      client.execute(request, handlePut(key));
    } catch (IOException e) {
      throw serviceException("Failed to save secret: key = " + key, e);
    }
  }

  @Override
  public void delete(String key) {
    validateKey(key);

    try (var client = httpClientBuilder.build()) {
      var request = new HttpDelete(secretUri + key);

      log.debug("Sending request to delete secret: key = {}", key);
      client.execute(request, handleDelete(key));
    } catch (IOException e) {
      throw serviceException("Failed to delete secret: key = " + key, e);
    }
  }

  private static String buildUri(FsspConfigProperties properties) throws URISyntaxException {
    return new URI(CS.removeEnd(properties.getAddress(), "/")
      + wrapIfMissing(properties.getSecretPath(), "/")).toString();
  }

  private static ResponseHandler<String> handleGet(String key) {
    return response ->
      switch (response.getStatusLine().getStatusCode()) {
        case HttpStatus.SC_OK -> {
          log.debug("Successfully retrieved secret: key = {}", key);
          yield parseSecureStoreEntry(response.getEntity()).getValue();
        }
        case HttpStatus.SC_NOT_FOUND ->
          throw new SecretNotFoundException("Secret not found: key = " + key);
        default ->
          throw serviceException("Failed to get secret: key = " + key, response);
      };
  }

  private static ResponseHandler<?> handlePut(String key) {
    return response -> {
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
        log.debug("Successfully saved secret: key = {}", key);
        return null;
      } else {
        throw serviceException("Failed to save secret: key = " + key, response);
      }
    };
  }

  private static ResponseHandler<?> handleDelete(String key) {
    return response -> {
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
        log.debug("Successfully deleted secret: key = {}", key);
        return null;
      } else {
        throw serviceException("Failed to delete secret: key = " + key, response);
      }
    };
  }

  private static SecureStoreServiceException serviceException(String mainMessage, HttpResponse response)
    throws IOException {
    var entity = response.getEntity();
    var statusCode = response.getStatusLine().getStatusCode();

    return new SecureStoreServiceException(mainMessage
      + ", status code = " + statusCode
      + ((entity != null) ? ", message = " + EntityUtils.toString(entity) : ""));
  }

  private static SecureStoreServiceException serviceException(String mainMessage, Exception e) {
    return new SecureStoreServiceException(mainMessage
      + ", error = " + e.getMessage(), e);
  }

  private static SecureStoreEntry parseSecureStoreEntry(HttpEntity entity) throws IOException {
    return OBJECT_MAPPER.readValue(entity.getContent(), SecureStoreEntry.class);
  }

  private static HttpEntity serializeSecureStoreEntry(SecureStoreEntry entry) throws IOException {
    return new StringEntity(OBJECT_MAPPER.writeValueAsString(entry), APPLICATION_JSON);
  }

  @Data
  @AllArgsConstructor(staticName = "of")
  private static final class SecureStoreEntry {

    private String key;
    private String value;
  }

  @NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
  private static final class HttpClientBuilderFactory {

    private static final int DEFAULT_REQUEST_CONNECT_TIMEOUT = 10000;
    private static final int DEFAULT_REQUEST_SOCKET_TIMEOUT = 5000;

    public static HttpClientBuilder create(FsspConfigProperties properties) {
      return createHttpClientBuilder(properties);
    }

    private static HttpClientBuilder createHttpClientBuilder(FsspConfigProperties properties) {
      var builder = HttpClientBuilder.create();

      RequestConfig requestConfig = RequestConfig.custom()
        .setConnectTimeout(DEFAULT_REQUEST_CONNECT_TIMEOUT)
        .setSocketTimeout(DEFAULT_REQUEST_SOCKET_TIMEOUT)
        .build();
      builder.setDefaultRequestConfig(requestConfig);

      var sslContext = buildSslContext(toTlsProperties(properties));
      builder.setSSLContext(sslContext);

      if (IS_HOSTNAME_VERIFICATION_DISABLED) {
        builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
      }

      return builder;
    }

    private static TlsProperties toTlsProperties(FsspConfigProperties properties) {
      return TlsProperties.builder()
        .enabled(true)
        .keyStore(Store.builder()
          .path(properties.getKeyStorePath())
          .type(properties.getKeyStoreFileType())
          .password(properties.getKeyStorePassword())
          .build())
        .trustStore(Store.builder()
          .path(properties.getTrustStorePath())
          .type(properties.getTrustStoreFileType())
          .password(properties.getTrustStorePassword())
          .build())
        .build();
    }
  }
}
