package org.folio.security.domain.model.descriptor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class EnvEntry {

  private String name;
  private String value;
  private String description;
}
