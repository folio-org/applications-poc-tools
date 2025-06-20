package org.folio.tools.store.properties;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.URL;
import org.springframework.validation.annotation.Validated;

@Data
@EqualsAndHashCode(callSuper = true)
@Validated
public class FsspProperties extends FsspConfigProperties {

  @URL
  private String address;
  @NotNull
  private Boolean enableSsl;
}
