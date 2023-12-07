package org.folio.security.integration.authtoken.configuration.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "application.okapi")
public class OkapiProperties {

  @NotNull(message = "OKAPI url should be defined")
  private String url;
  @NotNull(message = "mod-authtoken url should be defined")
  private String modAuthtokenUrl;
}
