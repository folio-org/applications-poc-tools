package org.folio.tools.store.properties;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.validation.annotation.Validated;

@Data
@EqualsAndHashCode(callSuper = true)
@Validated
public class SecureStoreProperties extends SecureStoreConfigProperties {

  /**
   * First segment of the secure store key, for example "prod" or "test".
   *
   * <p>For the Kafka topic prefix see org.folio.common.configuration.properties.FolioEnvironment#getEnvironment().
   */
  @NotEmpty
  @Pattern(regexp = "[\\w\\-]+", message = "Value must follow the pattern: '[\\w\\-]+'")
  private String environment;
}
