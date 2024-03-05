package org.folio.security.integration.keycloak.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "application.keycloak")
public class KeycloakProperties {

  private String url;
  private String impersonationClient;
  @NestedConfigurationProperty
  private KeycloakAdminProperties admin;
  @NestedConfigurationProperty
  private KeycloakClientProperties client;
  @NestedConfigurationProperty
  private KeycloakTlsProperties tls;
}
