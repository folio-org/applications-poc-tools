package org.folio.integration.kafka.consumer.filter.te;

import java.util.Objects;
import java.util.Set;
import lombok.extern.log4j.Log4j2;

/**
 * Service that delegates to {@link TenantEntitlementClient} to retrieve the set of tenants
 * currently entitled to receive messages for a specific module.
 */
@Log4j2
public class TenantEntitlementService {

  private final String moduleId;
  private final TenantEntitlementClient tenantEntitlementClient;

  /**
   * Creates a new service for the given module.
   *
   * @param moduleId                the module identifier; must not be {@code null}
   * @param tenantEntitlementClient the HTTP client used to query entitled tenants
   */
  public TenantEntitlementService(String moduleId, TenantEntitlementClient tenantEntitlementClient) {
    Objects.requireNonNull(moduleId, "Module ID must not be null");
    this.moduleId = moduleId;
    this.tenantEntitlementClient = tenantEntitlementClient;
  }

  /**
   * Fetches the tenants entitled for this module from the remote entitlement service.
   *
   * @return set of entitled tenant identifiers
   */
  public Set<String> getEnabledTenants() {
    var result = tenantEntitlementClient.lookupTenantsByModuleId(moduleId);
    log.debug("Tenants entitled for module: {}", result);
    return result;
  }
}
