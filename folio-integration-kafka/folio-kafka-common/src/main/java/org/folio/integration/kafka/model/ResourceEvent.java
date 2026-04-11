package org.folio.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.Nullable;

/**
 * Generic Kafka event envelope carrying metadata and an optional resource payload.
 *
 * <p>The type parameter {@code T} represents the domain-specific resource type. Both
 * {@link #newValue} and {@link #oldValue} are optional: a {@link ResourceEventType#CREATE CREATE}
 * event carries only {@link #newValue}, a {@link ResourceEventType#DELETE DELETE} event carries
 * only {@link #oldValue}, and an {@link ResourceEventType#UPDATE UPDATE} event may carry both.
 *
 * @param <T> the domain resource type carried in this event
 */
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
