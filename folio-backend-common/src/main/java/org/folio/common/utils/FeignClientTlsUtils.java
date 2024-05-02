package org.folio.common.utils;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.ResourceUtils.getFile;

import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.okhttp.OkHttpClient;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLInitializationException;
import org.folio.common.configuration.properties.TlsProperties;

@Slf4j
@UtilityClass
public class FeignClientTlsUtils {

  public static <T> T buildTargetFeignClient(okhttp3.OkHttpClient okHttpClient, Contract contract, Encoder encoder,
    Decoder decoder, TlsProperties tls, String url, Class<T> clientClass) {

    var builder = Feign.builder().contract(contract).encoder(encoder).decoder(decoder);
    builder.client(getOkHttpClient(okHttpClient, tls));
    return builder.target(clientClass, url);
  }

  public static OkHttpClient getOkHttpClient(okhttp3.OkHttpClient okHttpClient, TlsProperties tls) {
    var client = nonNull(tls) && tls.isEnabled() ? sslClient(okHttpClient.newBuilder(), tls) : okHttpClient;
    return new OkHttpClient(client);
  }

  private static okhttp3.OkHttpClient sslClient(okhttp3.OkHttpClient.Builder clientBuilder, TlsProperties tls) {
    log.debug("Creating OkHttpClient with SSL enabled...");
    if (isBlank(tls.getTrustStorePath()) || isBlank(tls.getTrustStorePassword())) {
      log.debug("Creating OkHttpClient for Public Trusted Certificates");
      return clientBuilder.build();
    }
    try {
      var keyStore = initKeyStore(tls);
      var trustManager = trustManager(keyStore);
      var sslSocketFactory = sslContext(trustManager).getSocketFactory();

      return clientBuilder
        .sslSocketFactory(sslSocketFactory, trustManager)
        .hostnameVerifier(NoopHostnameVerifier.INSTANCE)
        .build();
    } catch (Exception e) {
      log.error("Error creating OkHttpClient with SSL context", e);
      throw new SSLInitializationException("Error creating OkHttpClient with SSL context", e);
    }
  }

  private static KeyStore initKeyStore(TlsProperties tls)
    throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

    KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (var is = new FileInputStream(getFile(tls.getTrustStorePath()))) {
      trustStore.load(is, tls.getTrustStorePassword().toCharArray());
    }
    log.debug("Keystore initialized from file: keyStoreType = {}, file = {}", trustStore.getType(),
      tls.getTrustStorePath());
    return trustStore;
  }

  private static X509TrustManager trustManager(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(keyStore);

    TrustManager[] trustManagers = tmf.getTrustManagers();
    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
      throw new IllegalStateException("Unexpected default trust managers: " + Arrays.toString(trustManagers));
    }
    return (X509TrustManager) trustManagers[0];
  }

  private static SSLContext sslContext(X509TrustManager trustManager)
    throws NoSuchAlgorithmException, KeyManagementException {

    var sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] {trustManager}, null);
    log.debug("SSL context initialized: protocol = {}", sslContext.getProtocol());
    return sslContext;
  }
}
