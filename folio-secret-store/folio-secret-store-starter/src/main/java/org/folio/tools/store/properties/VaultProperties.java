package org.folio.tools.store.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.validation.annotation.Validated;

@Data
@EqualsAndHashCode(callSuper = true)
@Validated
public class VaultProperties extends VaultConfigProperties {

  @NotNull
  private String token;
  @NotNull
  private String address;
  @NotNull
  private Boolean enableSsl;
}
