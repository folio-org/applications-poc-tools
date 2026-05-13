package org.folio.integration.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ResourceEvent<T> implements TenantAwareEvent {

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

  public ResourceEvent<T> id(String id) {
    this.id = id;
    return this;
  }

  public ResourceEvent<T> type(ResourceEventType type) {
    this.type = type;
    return this;
  }

  public ResourceEvent<T> tenant(String tenant) {
    this.tenant = tenant;
    return this;
  }

  public ResourceEvent<T> resourceName(String resourceName) {
    this.resourceName = resourceName;
    return this;
  }

  public ResourceEvent<T> newValue(T newValue) {
    this.newValue = newValue;
    return this;
  }

  public ResourceEvent<T> oldValue(T oldValue) {
    this.oldValue = oldValue;
    return this;
  }
}
