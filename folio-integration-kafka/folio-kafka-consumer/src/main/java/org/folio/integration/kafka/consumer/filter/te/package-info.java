/**
 * Tenant entitlement client and service used by the Kafka consumer filtering layer.
 *
 * <p>Provides the
 * {@link org.folio.integration.kafka.consumer.filter.te.TenantEntitlementClient} Spring HTTP
 * interface and the
 * {@link org.folio.integration.kafka.consumer.filter.te.TenantEntitlementService} wrapper that
 * resolves the set of tenants currently entitled to receive messages for a given module.
 */
@NullMarked
package org.folio.integration.kafka.consumer.filter.te;

import org.jspecify.annotations.NullMarked;
