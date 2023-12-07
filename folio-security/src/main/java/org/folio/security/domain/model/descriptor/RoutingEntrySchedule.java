package org.folio.security.domain.model.descriptor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class RoutingEntrySchedule {

  private String cron;
  private String zone;
}
