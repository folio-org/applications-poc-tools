package org.folio.security.integration.keycloak.configuration.properties;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.common.configuration.properties.TlsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "application.keycloak")
public class KeycloakProperties {

  /**
   * Keycloak URL.
   */
  private String url;

  /**
   * Authentication JWT parser configuration settings.
   */
  private KeycloakJwtCacheProperties jwtCacheConfiguration = new KeycloakJwtCacheProperties();

  /**
   * Impersonation client name.
   */
  private String impersonationClient;

  /**
   * Keycloak admin client properties.
   */
  @NestedConfigurationProperty
  private KeycloakAdminProperties admin;

  /**
   * Keycloak client properties.
   */
  @NestedConfigurationProperty
  private KeycloakClientProperties client;

  /**
   * Keycloak TLS properties.
   */
  @NestedConfigurationProperty
  private TlsProperties tls;

  @Data
  @NoArgsConstructor
  public static class KeycloakJwtCacheProperties {

    /**
     * Defines if validation for JWT must be run to compare configuration URL and token issuer for keycloak.
     */
    private boolean validateUri = false;

    /**
     * Jwks refresh interval for realm JWT parser.
     */
    private int jwksRefreshInterval = 60;

    /**
     * Forced jwks refresh interval for realm JWT parser.
     *
     * <p>Applies for signing key rotation</p>
     */
    private int forcedJwksRefreshInterval = 60;
  }
}
