package org.folio.security.integration.keycloak.utils;

import static jakarta.ws.rs.client.ClientBuilder.newBuilder;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.stripToNull;
import static org.folio.common.utils.tls.Utils.IS_HOSTNAME_VERIFICATION_DISABLED;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.common.utils.tls.FeignClientTlsUtils;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

@Slf4j
@UtilityClass
public class ClientBuildUtils {

  private static final DefaultHostnameVerifier DEFAULT_HOSTNAME_VERIFIER = new DefaultHostnameVerifier();

  public static Keycloak buildKeycloakAdminClient(String clientSecret, KeycloakProperties properties) {
    var admin = properties.getAdmin();
    var builder = KeycloakBuilder.builder()
      .realm("master")
      .serverUrl(properties.getUrl())
      .clientId(admin.getClientId())
      .clientSecret(stripToNull(clientSecret))
      .username(stripToNull(admin.getUsername()))
      .password(stripToNull(admin.getPassword()))
      .grantType(admin.getGrantType());

    if (properties.getTls() != null && properties.getTls().isEnabled()) {
      builder.resteasyClient(buildResteasyClient(properties.getTls()));
    }
    return builder.build();
  }

  private static ResteasyClient buildResteasyClient(TlsProperties tls) {
    var clientBuilder = newBuilder()
      .hostnameVerifier(IS_HOSTNAME_VERIFICATION_DISABLED ? NoopHostnameVerifier.INSTANCE : DEFAULT_HOSTNAME_VERIFIER)
      .register(JacksonProvider.class);
    if (isBlank(tls.getTrustStorePath())) {
      log.debug("Creating ResteasyClient for Public Trusted Certificates");
      return (ResteasyClient) clientBuilder.build();
    }
    return (ResteasyClient) clientBuilder.sslContext(FeignClientTlsUtils.buildSslContext(tls)).build();
  }
}
