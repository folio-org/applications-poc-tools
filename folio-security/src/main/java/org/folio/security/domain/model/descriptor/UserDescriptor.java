package org.folio.security.domain.model.descriptor;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class UserDescriptor {

  private String type;
  private List<String> permissions;
}
