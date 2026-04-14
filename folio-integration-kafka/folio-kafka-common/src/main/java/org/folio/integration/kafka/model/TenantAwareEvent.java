package org.folio.integration.kafka.model;

import org.jspecify.annotations.Nullable;

/**
 * Contract for Kafka event payloads that carry a tenant identifier.
 *
 * <p>Implement this interface to make a custom event type compatible with
 * {@link org.folio.integration.kafka.consumer.filter.EnabledTenantMessageFilter}.
 * {@link ResourceEvent} implements this interface out of the box.
 */
public interface TenantAwareEvent {

  /**
   * Returns the tenant identifier for this event.
   *
   * <p>A {@code null} or blank value is treated as absent by
   * {@link org.folio.integration.kafka.consumer.filter.EnabledTenantMessageFilter}:
   * the record is accepted without consulting the entitlement service and a warning is logged.
   *
   * @return the tenant name, or {@code null} / blank for malformed events
   */
  @Nullable String getTenant();
}
