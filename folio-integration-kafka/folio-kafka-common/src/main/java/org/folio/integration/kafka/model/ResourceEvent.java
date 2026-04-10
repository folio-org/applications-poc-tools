package org.folio.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
@Builder
public class ResourceEvent<T> {

  /**
   * Resource identifier.
   */
  private String id;

  /**
   * Event type.
   */
  private ResourceEventType type;

  /**
   * Tenant identifier (name).
   */
  private String tenant;

  /**
   * Name of resource.
   */
  private String resourceName;

  /**
   * New value (if resource is created or updated).
   */
  @Nullable
  @JsonProperty("new")
  private T newValue;

  /**
   * Previous version value (if resource was updated or deleted).
   */
  @Nullable
  @JsonProperty("old")
  private T oldValue;
}
