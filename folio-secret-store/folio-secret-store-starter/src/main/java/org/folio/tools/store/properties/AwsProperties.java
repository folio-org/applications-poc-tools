package org.folio.tools.store.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.validation.annotation.Validated;

@Data
@EqualsAndHashCode(callSuper = true)
@Validated
public class AwsProperties extends AwsConfigProperties {

  @NotNull
  private String region;
  @NotNull
  private Boolean useIam;
}
