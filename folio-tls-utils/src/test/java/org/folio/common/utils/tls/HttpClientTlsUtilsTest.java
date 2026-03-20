package org.folio.common.utils.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.tls.HttpClientTlsUtils.buildHttpServiceClient;
import static org.folio.common.utils.tls.HttpClientTlsUtils.getRequestFactory;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.common.configuration.properties.TlsProperties;
import org.folio.common.utils.exception.SslInitializationException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@UnitTest
class HttpClientTlsUtilsTest {

  @Test
  void buildHttpServiceClient_positive_tlsDisabled() {
    var client = buildHttpServiceClient(
      RestClient.builder(),
      TlsProperties.of(false, null, null, null),
      "http://localhost:8080",
      TestClient.class);

    assertThat(client).isNotNull();
  }

  @Test
  void buildHttpServiceClient_positive_tlsNull() {
    var client = buildHttpServiceClient(
      RestClient.builder(),
      null,
      "http://localhost:8080",
      TestClient.class);

    assertThat(client).isNotNull();
  }

  @Test
  void buildHttpServiceClient_positive_tlsEnabled() {
    var client = buildHttpServiceClient(
      RestClient.builder(),
      getEnabledTlsProperties(),
      "https://localhost:8443",
      TestClient.class);

    assertThat(client).isNotNull();
  }

  @Test
  void buildHttpServiceClient_negative_tlsEnabledWithBlankTruststorePath() {
    var tls = TlsProperties.of(true, "", null, null);
    assertThrows(SslInitializationException.class, () -> buildHttpServiceClient(
      RestClient.builder(),
      tls,
      "https://localhost:8443",
      TestClient.class));
  }

  @Test
  void buildHttpServiceClient_negative_invalidTruststorePath() {
    var invalidTlsProperties = getInvalidTlsProperties();
    assertThrows(SslInitializationException.class, () -> buildHttpServiceClient(
      RestClient.builder(),
      invalidTlsProperties,
      "https://localhost:8443",
      TestClient.class));
  }

  @Test
  void getRequestFactory_positive_tlsNull() {
    var factory = getRequestFactory(null);

    assertThat(factory).isInstanceOf(SimpleClientHttpRequestFactory.class);
  }

  @Test
  void getRequestFactory_positive_tlsDisabled() {
    var factory = getRequestFactory(TlsProperties.of(false, null, null, null));

    assertThat(factory).isInstanceOf(SimpleClientHttpRequestFactory.class);
  }

  @Test
  void getRequestFactory_positive_tlsEnabledWithTruststorePath() {
    var factory = getRequestFactory(getEnabledTlsProperties());

    assertThat(factory).isInstanceOf(JdkClientHttpRequestFactory.class);
  }

  @Test
  void getRequestFactory_negative_tlsEnabledWithBlankTruststorePath() {
    var tls = TlsProperties.of(true, "", null, null);

    assertThrows(SslInitializationException.class, () -> getRequestFactory(tls));
  }

  @Test
  void getRequestFactory_negative_tlsEnabledWithNullTruststorePath() {
    var tls = TlsProperties.of(true, null, null, null);

    assertThrows(NullPointerException.class, () -> getRequestFactory(tls));
  }

  @Test
  void getRequestFactory_negative_tlsEnabledWithInvalidTruststorePath() {
    var tls = getInvalidTlsProperties();

    assertThrows(SslInitializationException.class, () -> getRequestFactory(tls));
  }

  private static TlsProperties getEnabledTlsProperties() {
    return TlsProperties.of(true, "classpath:certificates/test.truststore.jks", "secretpassword", "JKS");
  }

  private static TlsProperties getInvalidTlsProperties() {
    return TlsProperties.of(true, "dummy", "dummy", "");
  }

  @HttpExchange
  interface TestClient {
    @GetExchange("/test/{id}")
    String getTest(@PathVariable String id);
  }
}
