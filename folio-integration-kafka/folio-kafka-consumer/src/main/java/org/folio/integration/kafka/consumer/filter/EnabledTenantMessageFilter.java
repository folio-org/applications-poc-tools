package org.folio.integration.kafka.consumer.filter;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementService;
import org.folio.integration.kafka.model.TenantAwareEvent;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

/**
 * {@link RecordFilterStrategy} that accepts only records whose tenant is currently entitled
 * to receive messages for the current module.
 *
 * <p>On each call to {@link #filter}, the entitled-tenant set is fetched via
 * {@link TenantEntitlementService#getEnabledTenants()}, and the configured
 * {@link DisabledTenantStrategy strategies} are applied:
 * <ul>
 *   <li>{@code tenantDisabledStrategy} — used when the tenant set is non-empty but does not
 *       contain the record's tenant;</li>
 *   <li>{@code allTenantsDisabledStrategy} — used when no tenants are entitled at all.</li>
 * </ul>
 *
 * @param <K> the Kafka record key type
 * @param <V> the record value type; must implement {@link TenantAwareEvent}
 */
@Log4j2
public class EnabledTenantMessageFilter<K, V extends TenantAwareEvent> implements RecordFilterStrategy<K, V> {

  private final String moduleId;
  private final TenantEntitlementService tenantEntitlementService;
  private final boolean ignoreEmptyBatch;
  private final DisabledTenantStrategy tenantDisabledStrategy;
  private final DisabledTenantStrategy allTenantsDisabledStrategy;

  /**
   * Creates a new filter instance.
   *
   * @param moduleId                   the current module identifier; must not be blank
   * @param tenantEntitlementService   service used to resolve the entitled-tenant set
   * @param ignoreEmptyBatch           whether to signal Kafka to skip delivery on empty batches
   * @param tenantDisabledStrategy     strategy when a single tenant is not entitled
   * @param allTenantsDisabledStrategy strategy when no tenants are entitled
   * @throws IllegalArgumentException if {@code moduleId} is blank
   */
  public EnabledTenantMessageFilter(String moduleId, TenantEntitlementService tenantEntitlementService,
    boolean ignoreEmptyBatch, DisabledTenantStrategy tenantDisabledStrategy,
    DisabledTenantStrategy allTenantsDisabledStrategy) {
    if (isBlank(moduleId)) {
      throw new IllegalArgumentException("Module ID must not be blank");
    }

    Objects.requireNonNull(tenantDisabledStrategy, "tenantDisabledStrategy must not be null");
    Objects.requireNonNull(allTenantsDisabledStrategy, "allTenantDisabledStrategy must not be null");

    this.moduleId = moduleId;
    this.tenantEntitlementService = tenantEntitlementService;
    this.ignoreEmptyBatch = ignoreEmptyBatch;
    this.tenantDisabledStrategy = tenantDisabledStrategy;
    this.allTenantsDisabledStrategy = allTenantsDisabledStrategy;
  }

  @Override
  public boolean filter(ConsumerRecord<K, V> consumerRecord) {
    var key = consumerRecord.key();
    var value = consumerRecord.value();
    var tenant = value.getTenant();

    if (isBlank(tenant)) {
      log.warn("Received message with blank tenant: messageKey = {}, tenant = '{}'. Filter won't be applied.",
        key, tenant);
      return false;
    }

    log.debug("Filtering message for tenant: messageKey = {}, tenant = {}", key, tenant);

    var enabledTenants = tenantEntitlementService.getEnabledTenants();

    var result = filterByEnabledTenants(enabledTenants, tenant);

    log.debug("Message for tenant is {}: messageKey = {}, tenant = {}",
      result ? "filtered out" : "accepted", key, tenant);

    return result;
  }

  private boolean filterByEnabledTenants(Set<String> enabledTenants, String tenant) {
    if (isEmpty(enabledTenants)) {
      log.warn("No tenants are enabled for the module. Applying 'no enabled tenants' strategy: {}",
        allTenantsDisabledStrategy);

      return applyStrategy(allTenantsDisabledStrategy, () -> TenantsAreDisabledException.of(moduleId));
    } else {
      var notEnabled = !enabledTenants.contains(tenant);

      return notEnabled && applyStrategy(tenantDisabledStrategy, () -> TenantIsDisabledException.of(tenant, moduleId));
    }
  }

  @Override
  public boolean ignoreEmptyBatch() {
    return ignoreEmptyBatch;
  }

  private static boolean applyStrategy(DisabledTenantStrategy strategy, Supplier<RuntimeException> exceptionSupplier) {
    return switch (strategy) {
      case ACCEPT -> false;
      case SKIP -> true;
      case FAIL -> throw exceptionSupplier.get();
    };
  }
}
