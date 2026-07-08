package org.folio.security.integration.keycloak.configuration.properties;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties
public class KeycloakAdminProperties {

  private String clientId;
  private String username;
  private String password;
  private String grantType;

  /**
   * Connect timeout for the Keycloak admin client HTTP calls.
   *
   * <p>Bounds how long a request waits to establish a connection before failing. Defaults to {@code 10s}.</p>
   */
  private Duration connectTimeout = Duration.ofSeconds(10);

  /**
   * Read/socket timeout for the Keycloak admin client HTTP calls.
   *
   * <p>Bounds how long a request waits for a response after the connection is established before failing.
   * Defaults to {@code 60s}.</p>
   */
  private Duration readTimeout = Duration.ofSeconds(60);
}
