package org.folio.security.integration.keycloak.utils;

import static jakarta.ws.rs.client.ClientBuilder.newBuilder;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.stripToNull;
import static org.apache.http.conn.ssl.NoopHostnameVerifier.INSTANCE;
import static org.apache.http.ssl.SSLContextBuilder.create;
import static org.springframework.util.ResourceUtils.getFile;

import javax.net.ssl.SSLContext;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLInitializationException;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

@Slf4j
@UtilityClass
public class ClientBuildUtils {

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

  public static SSLContext buildSslContext(TlsProperties properties) {
    var trustStorePath = requireNonNull(properties.getTrustStorePath(), "Trust store path is not defined");
    var trustStorePassword = requireNonNull(properties.getTrustStorePassword(), "Trust store password is not defined");
    try {
      return create()
        .loadTrustMaterial(getFile(trustStorePath), trustStorePassword.toCharArray())
        .build();
    } catch (Exception e) {
      log.error("Error creating SSL context", e);
      throw new SSLInitializationException("Error creating SSL context", e);
    }
  }

  private static ResteasyClient buildResteasyClient(TlsProperties properties) {
    return (ResteasyClient) newBuilder().sslContext(buildSslContext(properties)).hostnameVerifier(INSTANCE).build();
  }
}
