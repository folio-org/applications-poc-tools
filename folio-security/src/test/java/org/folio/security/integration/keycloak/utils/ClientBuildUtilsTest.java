package org.folio.security.integration.keycloak.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.FeignClientTlsUtils.buildTargetFeignClient;
import static org.folio.security.integration.keycloak.utils.ClientBuildUtils.buildKeycloakAdminClient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakAdminProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakClientProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ClientBuildUtilsTest {

  private static final Contract CONTRACT = mock(Contract.class);
  private static final Encoder ENCODER = mock(Encoder.class);
  private static final Decoder DECODER = mock(Decoder.class);
  private final OkHttpClient okHttpClient = mock(OkHttpClient.class);

  @Test
  void buildTargetFeignClient_positive_tlsDisabled() {
    var properties = keycloakProperties(false);
    var keycloakAuthClient = buildTargetFeignClient(
      okHttpClient, CONTRACT, ENCODER, DECODER, properties.getTls(), properties.getUrl(), KeycloakAuthClient.class);

    assertThat(keycloakAuthClient)
      .isNotNull()
      .isInstanceOf(KeycloakAuthClient.class);
  }

  @Test
  void buildTargetFeignClient_positive_tlsEnabled() {
    when(okHttpClient.newBuilder()).thenReturn(new Builder());
    var properties = keycloakProperties(true);
    var keycloakAuthClient = buildTargetFeignClient(
      okHttpClient, CONTRACT, ENCODER, DECODER, properties.getTls(), properties.getUrl(), KeycloakAuthClient.class);

    assertThat(keycloakAuthClient)
      .isNotNull()
      .isInstanceOf(KeycloakAuthClient.class);
  }

  @Test
  void buildTargetFeignClient_positive_tlsPropertiesIsNull() {
    var properties = keycloakProperties(true);
    properties.setTls(null);
    var keycloakAuthClient = buildTargetFeignClient(
      okHttpClient, CONTRACT, ENCODER, DECODER, properties.getTls(), properties.getUrl(), KeycloakAuthClient.class);

    assertThat(keycloakAuthClient)
      .isNotNull()
      .isInstanceOf(KeycloakAuthClient.class);
  }

  @Test
  void buildKeycloakAdminClient_positive_tlsDisabled() {
    var keycloakProperties = keycloakProperties(false);
    var keycloakAdminClient = buildKeycloakAdminClient("secretPassword", keycloakProperties);

    assertThat(keycloakAdminClient).isNotNull();
  }

  @Test
  void buildKeycloakAdminClient_positive_tlsEnabled() {
    var keycloakProperties = keycloakProperties(true);
    var keycloakAdminClient = buildKeycloakAdminClient("secretPassword", keycloakProperties);

    assertThat(keycloakAdminClient).isNotNull();
  }

  @Test
  void buildKeycloakAdminClient_positive_tlsPropertiesIsNull() {
    var keycloakProperties = keycloakProperties(true);
    keycloakProperties.setTls(null);
    var keycloakAdminClient = buildKeycloakAdminClient("secretPassword", keycloakProperties);

    assertThat(keycloakAdminClient).isNotNull();
  }

  private static KeycloakProperties keycloakProperties(boolean tlsEnabled) {
    var properties = new KeycloakProperties();
    properties.setImpersonationClient("impersonation-client");
    properties.setUrl("http://localhost:8080/auth");

    var admin = new KeycloakAdminProperties();
    admin.setClientId("admin-client");
    admin.setUsername("folio");
    admin.setPassword("folio");
    admin.setGrantType("client_credentials");
    properties.setAdmin(admin);

    var client = new KeycloakClientProperties();
    client.setClientId("super-client");
    properties.setClient(client);

    var tls = new TlsProperties();
    tls.setEnabled(tlsEnabled);
    tls.setTrustStorePath("classpath:certificates/test.truststore.jks");
    tls.setTrustStorePassword("secretpassword");
    tls.setTrustStoreType("JKS");
    properties.setTls(tls);
    return properties;
  }
}
