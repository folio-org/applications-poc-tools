package org.folio.common.configuration.properties;

import static org.apache.commons.lang3.StringUtils.defaultString;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ConfigurationProperties
public class TlsProperties {

  private boolean enabled;
  private String trustStorePath;
  private String trustStorePassword;
  private String trustStoreType;

  public String getTrustStorePassword() {
    return defaultString(trustStorePassword);
  }
}
