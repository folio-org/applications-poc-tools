package org.folio.security.integration.keycloak.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.security.integration.keycloak.utils.ClientBuildUtils.buildKeycloakAdminClient;
import static org.folio.security.integration.keycloak.utils.ClientBuildUtils.buildSslContext;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.http.ssl.SSLInitializationException;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakAdminProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakClientProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ClientBuildUtilsTest {

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
  void buildKeycloakAdminClient_positive_withoutTruststore() {
    var keycloakProperties = keycloakProperties(true);
    keycloakProperties.getTls().setTrustStorePath("");
    var keycloakAdminClient = buildKeycloakAdminClient("secretPassword", keycloakProperties);

    assertThat(keycloakAdminClient).isNotNull();
  }

  @Test
  void buildSslContext_positive() {
    var tls = new TlsProperties();
    tls.setTrustStorePath("classpath:certificates/test.truststore.jks");
    tls.setTrustStorePassword("secretpassword");
    tls.setTrustStoreType("JKS");
    assertThat(buildSslContext(tls)).isNotNull();
  }

  @Test
  void buildSslContext_negative() {
    var tls = new TlsProperties();
    tls.setTrustStorePath("");
    assertThrows(SSLInitializationException.class, () -> buildSslContext(tls));
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
