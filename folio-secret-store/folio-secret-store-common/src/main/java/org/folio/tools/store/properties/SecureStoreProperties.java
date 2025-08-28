package org.folio.tools.store.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Configuration
@NoArgsConstructor
public class SecureStoreProperties {

  /**
   * First segment of the secure store key, for example "prod" or "test".
   *
   * <p>For the Kafka topic prefix see org.folio.common.configuration.properties.FolioEnvironment#getEnvironment().
   */
  @NotEmpty
  @Pattern(regexp = "[\\w\\-]+", message = "Value must follow the pattern: '[\\w\\-]+'")
  @Value("${application.secure-store.environment}")
  private String secureStoreEnvironment;
}
