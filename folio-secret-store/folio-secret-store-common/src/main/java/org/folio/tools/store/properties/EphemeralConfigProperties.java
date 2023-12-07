package org.folio.tools.store.properties;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EphemeralConfigProperties {

  /**
   * Content for populating ephemeral store.
   */
  private Map<String, String> content;
}
