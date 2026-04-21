package org.folio.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Kafka event type describing the lifecycle operation performed on a resource.
 */
@Getter
@RequiredArgsConstructor
public enum ResourceEventType {

  /** The resource was updated. */
  UPDATE("UPDATE"),

  /** The resource was created. */
  CREATE("CREATE"),

  /** The resource instance was deleted. */
  DELETE("DELETE"),

  /** All resource instances were deleted in bulk. */
  DELETE_ALL("DELETE_ALL");

  @JsonValue
  private final String value;
}
