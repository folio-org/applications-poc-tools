package org.folio.security.integration.keycloak.utils;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.stripToNull;
import static org.apache.http.ssl.SSLContextBuilder.create;
import static org.springframework.util.ResourceUtils.getFile;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import jakarta.ws.rs.client.ClientBuilder;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLInitializationException;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakTlsProperties;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

@Slf4j
@UtilityClass
public class ClientBuildUtils {

  public static <T> T buildTargetFeignClient(Contract contract, Encoder encoder, Decoder decoder,
    KeycloakProperties properties, Class<T> clientClass) {
    var builder = Feign.builder()
      .contract(contract).encoder(encoder).decoder(decoder);

    if (properties.getTls() != null && properties.getTls().isEnabled()) {
      builder.client(new Client.Default(createSslContext(properties.getTls()), new NoopHostnameVerifier()));
    }

    return builder.target(clientClass, properties.getUrl());
  }

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

  private static ResteasyClient buildResteasyClient(KeycloakTlsProperties properties) {
    return (ResteasyClient) ClientBuilder.newBuilder().sslContext(getSslContext(properties)).build();
  }

  private static SSLSocketFactory createSslContext(KeycloakTlsProperties properties) {
    return getSslContext(properties).getSocketFactory();
  }

  private static SSLContext getSslContext(KeycloakTlsProperties properties) {
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
}
