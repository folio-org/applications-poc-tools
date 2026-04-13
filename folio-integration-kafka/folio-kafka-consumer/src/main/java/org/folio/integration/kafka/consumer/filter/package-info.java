/**
 * Kafka consumer message filtering support.
 *
 * <p>Contains the {@link org.folio.integration.kafka.consumer.filter.EnabledTenantMessageFilter}
 * {@link org.springframework.kafka.listener.adapter.RecordFilterStrategy} implementation, the
 * {@link org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy} enum controlling
 * per-message behaviour when a tenant is not entitled, and the typed exceptions thrown when the
 * {@link org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy#FAIL FAIL} strategy
 * is active.
 */
@NullMarked
package org.folio.integration.kafka.consumer.filter;

import org.jspecify.annotations.NullMarked;
