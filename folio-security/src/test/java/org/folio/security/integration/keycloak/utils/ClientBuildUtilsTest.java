package org.folio.security.integration.keycloak.utils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.security.integration.keycloak.utils.ClientBuildUtils.buildKeycloakAdminClient;
import static org.folio.security.integration.keycloak.utils.ClientBuildUtils.configureClientBuilder;

import java.time.Duration;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakAdminProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakClientProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.test.types.UnitTest;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.jupiter.api.Test;

@UnitTest
class ClientBuildUtilsTest {

  @Test
  void configureClientBuilder_positive_defaultTimeouts() {
    var keycloakProperties = keycloakProperties(false);
    var clientBuilder = (ResteasyClientBuilder) configureClientBuilder(keycloakProperties);

    assertThat(clientBuilder.getConnectionTimeout(MILLISECONDS)).isEqualTo(10_000L);
    assertThat(clientBuilder.getReadTimeout(MILLISECONDS)).isEqualTo(60_000L);
  }

  @Test
  void configureClientBuilder_positive_customTimeouts() {
    var keycloakProperties = keycloakProperties(true);
    keycloakProperties.getAdmin().setConnectTimeout(Duration.ofSeconds(3));
    keycloakProperties.getAdmin().setReadTimeout(Duration.ofSeconds(15));
    var clientBuilder = (ResteasyClientBuilder) configureClientBuilder(keycloakProperties);

    assertThat(clientBuilder.getConnectionTimeout(MILLISECONDS)).isEqualTo(3_000L);
    assertThat(clientBuilder.getReadTimeout(MILLISECONDS)).isEqualTo(15_000L);
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
  void buildKeycloakAdminClient_positive_withoutTruststore() {
    var keycloakProperties = keycloakProperties(true);
    keycloakProperties.getTls().setTrustStorePath("");
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
