package org.folio.integration.kafka.model;

import org.jspecify.annotations.Nullable;

/**
 * Contract for Kafka event payloads that carry a tenant identifier.
 * {@link ResourceEvent} implements this interface out of the box.
 */
public interface TenantAwareEvent {

  /**
   * Returns the tenant identifier for this event.
   *
   * @return the tenant name, or {@code null} / blank for malformed events
   */
  @Nullable String getTenant();
}
