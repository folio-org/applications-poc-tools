package org.folio.integration.kafka.consumer.filter;

/**
 * Strategy applied when a tenant is not entitled to receive messages for the current module.
 *
 * <p>Used in two distinct contexts within {@link EnabledTenantMessageFilter}:
 * <ul>
 *   <li>{@code tenantDisabledStrategy} — applied when the entitled-tenant set is non-empty
 *       but does not contain the message tenant;</li>
 *   <li>{@code allTenantsDisabledStrategy} — applied when the entitled-tenant set is empty
 *       (no tenant is currently entitled).</li>
 * </ul>
 */
public enum DisabledTenantStrategy {

  /** Accept the record and let the listener process it. */
  ACCEPT,

  /** Silently discard the record (filter it out). */
  SKIP,

  /** Throw a typed exception to signal the unexpected state. */
  FAIL
}
