package org.folio.common.domain.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ConsumesEvent {

  private String event;
  private Boolean optional = false;

  @NotNull
  public String getEvent() {
    return event;
  }

  public ConsumesEvent event(String event) {
    this.event = event;
    return this;
  }

  @NotNull
  public Boolean getOptional() {
    return optional;
  }

  public ConsumesEvent optional(Boolean optional) {
    this.optional = optional;
    return this;
  }
}
