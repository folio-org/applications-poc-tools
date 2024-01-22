package org.folio.tools.kong.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "application.kong")
public class KongConfigurationProperties {

  /**
   * Defines if application manager is integrated with Kong API Gateway.
   */
  private boolean enabled;

  /**
   * Provides Kong API Gateway admin URL.
   */
  private String url;

  /**
   * Module URL for self-registration.
   */
  private String moduleSelfUrl;

  /**
   * Defines if module must be registered.
   */
  private boolean registerModule;
}
