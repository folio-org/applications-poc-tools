package org.folio.security.integration.keycloak.configuration.properties;

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
}
