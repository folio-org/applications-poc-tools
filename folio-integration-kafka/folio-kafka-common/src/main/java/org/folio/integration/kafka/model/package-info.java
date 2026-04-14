/**
 * Domain model classes for Kafka resource events.
 *
 * <p>Contains the following public types, shared between Kafka producers and consumers:
 * <ul>
 *   <li>{@link org.folio.integration.kafka.model.TenantAwareEvent} — contract for event payloads
 *       that carry a tenant identifier; used as the type bound by the consumer filter;</li>
 *   <li>{@link org.folio.integration.kafka.model.ResourceEvent} — generic event envelope
 *       implementing {@code TenantAwareEvent} and carrying metadata and an optional resource
 *       payload;</li>
 *   <li>{@link org.folio.integration.kafka.model.ResourceEventType} — lifecycle operation
 *       enumeration ({@code CREATE}, {@code UPDATE}, {@code DELETE}, {@code DELETE_ALL}).</li>
 * </ul>
 */
@NullMarked
package org.folio.integration.kafka.model;

import org.jspecify.annotations.NullMarked;
