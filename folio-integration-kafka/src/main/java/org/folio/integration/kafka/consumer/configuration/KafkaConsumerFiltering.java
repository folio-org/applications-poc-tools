package org.folio.integration.kafka.consumer.configuration;

import static org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy.FAIL;
import static org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy.SKIP;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy;

/**
 * Filtering configuration nested under {@code application.kafka.consumer.filtering}.
 *
 * <p>Controls the behaviour of the tenant-aware message filter registered by
 * {@link org.folio.integration.kafka.consumer.configuration.KafkaConsumerFilteringConfiguration}.
 */
@Data
public class KafkaConsumerFiltering {

  /** Tenant-entitlement filter settings. */
  private TenantFilter tenantFilter = new TenantFilter();

  /**
   * Per-listener tenant-entitlement filtering settings.
   */
  @Data
  public static class TenantFilter {

    /**
     * Whether tenant-entitlement filtering is active; must be explicitly set to {@code true}.
     */
    private boolean enabled = false;

    /**
     * When {@code true}, signals Kafka to skip delivery when the polled batch is empty.
     */
    private boolean ignoreEmptyBatch = true;

    /**
     * Strategy applied when a single tenant in a message is not entitled.
     * Defaults to {@link DisabledTenantStrategy#SKIP SKIP}.
     */
    private @NotNull DisabledTenantStrategy tenantDisabledStrategy = SKIP;

    /**
     * Strategy applied when no tenants are entitled for the module.
     * Defaults to {@link DisabledTenantStrategy#FAIL FAIL}.
     */
    private @NotNull DisabledTenantStrategy allTenantsDisabledStrategy = FAIL;
  }
}
