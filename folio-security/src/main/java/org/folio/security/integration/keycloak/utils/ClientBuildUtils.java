package org.folio.security.integration.keycloak.utils;

import static jakarta.ws.rs.client.ClientBuilder.newBuilder;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.stripToNull;
import static org.folio.common.utils.tls.Utils.IS_HOSTNAME_VERIFICATION_DISABLED;
import static org.folio.common.utils.tls.Utils.buildSslContext;

import jakarta.ws.rs.client.ClientBuilder;
import java.time.Duration;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakAdminProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

@Slf4j
@UtilityClass
public class ClientBuildUtils {

  private static final DefaultHostnameVerifier DEFAULT_HOSTNAME_VERIFIER = new DefaultHostnameVerifier();
  private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);

  public static Keycloak buildKeycloakAdminClient(String clientSecret, KeycloakProperties properties) {
    var admin = properties.getAdmin();
    return KeycloakBuilder.builder()
      .realm("master")
      .serverUrl(properties.getUrl())
      .clientId(admin.getClientId())
      .clientSecret(stripToNull(clientSecret))
      .username(stripToNull(admin.getUsername()))
      .password(stripToNull(admin.getPassword()))
      .grantType(admin.getGrantType())
      .resteasyClient(buildResteasyClient(properties))
      .build();
  }

  static ResteasyClient buildResteasyClient(KeycloakProperties properties) {
    return (ResteasyClient) configureClientBuilder(properties).build();
  }

  static ClientBuilder configureClientBuilder(KeycloakProperties properties) {
    var admin = properties.getAdmin();
    var clientBuilder = newBuilder()
      .connectTimeout(resolveConnectTimeout(admin).toMillis(), MILLISECONDS)
      .readTimeout(resolveReadTimeout(admin).toMillis(), MILLISECONDS)
      .register(JacksonProvider.class);

    var tls = properties.getTls();
    if (tls != null && tls.isEnabled()) {
      clientBuilder.hostnameVerifier(
        IS_HOSTNAME_VERIFICATION_DISABLED ? NoopHostnameVerifier.INSTANCE : DEFAULT_HOSTNAME_VERIFIER);
      if (isBlank(tls.getTrustStorePath())) {
        log.debug("Creating ResteasyClient for Public Trusted Certificates");
      } else {
        clientBuilder.sslContext(buildSslContext(tls));
      }
    }
    return clientBuilder;
  }

  private static Duration resolveConnectTimeout(KeycloakAdminProperties admin) {
    return admin != null && admin.getConnectTimeout() != null
      ? admin.getConnectTimeout()
      : DEFAULT_CONNECT_TIMEOUT;
  }

  private static Duration resolveReadTimeout(KeycloakAdminProperties admin) {
    return admin != null && admin.getReadTimeout() != null
      ? admin.getReadTimeout()
      : DEFAULT_READ_TIMEOUT;
  }
}
