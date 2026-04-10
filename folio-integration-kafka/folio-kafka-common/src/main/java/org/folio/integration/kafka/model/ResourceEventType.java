package org.folio.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResourceEventType {

  UPDATE("UPDATE"),
  CREATE("CREATE"),
  DELETE("DELETE"),
  DELETE_ALL("DELETE_ALL");

  @JsonValue
  private final String value;
}
