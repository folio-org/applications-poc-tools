package org.folio.security.integration.keycloak.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties
public class KeycloakTlsProperties {

  private boolean enabled;
  private String trustStorePath;
  private String trustStorePassword;
}
