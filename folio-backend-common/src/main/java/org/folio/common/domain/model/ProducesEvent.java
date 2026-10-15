package org.folio.common.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProducesEvent {

  private String event;

  @NotNull
  public String getEvent() {
    return event;
  }

  public ProducesEvent event(String event) {
    this.event = event;
    return this;
  }
}
