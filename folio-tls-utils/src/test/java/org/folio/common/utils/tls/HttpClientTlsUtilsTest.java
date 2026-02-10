package org.folio.common.utils.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.tls.HttpClientTlsUtils.buildHttpServiceClient;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.folio.common.configuration.properties.TlsProperties;
import org.folio.common.utils.exception.SslInitializationException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@UnitTest
class HttpClientTlsUtilsTest {

  @Test
  void buildHttpServiceClient_positive_tlsDisabled() {
    var restClientBuilder = RestClient.builder();

    var client = buildHttpServiceClient(
      restClientBuilder,
      TlsProperties.of(false, null, null, null),
      "http://localhost:8080",
      TestClient.class);

    assertThat(client).isNotNull();
  }

  @Test
  void buildHttpServiceClient_positive_tlsNull() {
    var restClientBuilder = RestClient.builder();

    var client = buildHttpServiceClient(
      restClientBuilder,
      null,
      "http://localhost:8080",
      TestClient.class);

    assertThat(client).isNotNull();
  }

  @Test
  void buildHttpServiceClient_positive_tlsEnabled() {
    var restClientBuilder = RestClient.builder();

    var client = buildHttpServiceClient(
      restClientBuilder,
      getEnabledTlsProperties(),
      "https://localhost:8443",
      TestClient.class);

    assertThat(client).isNotNull();
  }

  @Test
  void buildHttpServiceClient_positive_tlsEnabledWithBlankTruststorePath() {
    var restClientBuilder = RestClient.builder();

    var client = buildHttpServiceClient(
      restClientBuilder,
      TlsProperties.of(true, "", null, null),
      "https://localhost:8443",
      TestClient.class);

    assertThat(client).isNotNull();
  }

  @Test
  void buildHttpServiceClient_negative_invalidTruststorePath() {
    var restClientBuilder = RestClient.builder();
    var invalidTlsProperties = getInvalidTlsProperties();
    assertThrows(SslInitializationException.class, () -> buildHttpServiceClient(
      restClientBuilder,
      invalidTlsProperties,
      "https://localhost:8443",
      TestClient.class));
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
