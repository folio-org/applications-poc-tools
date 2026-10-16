package org.folio.integration.kafka.consumer.filter;

import static org.apache.commons.lang3.Validate.notBlank;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.log4j.Log4j2;
import org.folio.spring.FolioExecutionContext;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Records which module version initialized a tenant and resolves whether a Kafka event belongs to
 * another version of the module.
 */
@Log4j2
public class TenantModuleVersionService {

  private final TenantModuleVersionRepository repository;
  private final TenantInitializationDetector initializationDetector;
  private final FolioExecutionContext folioExecutionContext;
  private final String moduleId;
  private final Set<String> tenantsWithMarkerTable = ConcurrentHashMap.newKeySet();

  /**
   * Creates a tenant module-version service.
   *
   * @param repository stores and retrieves module-version markers
   * @param initializationDetector determines whether a tenant schema is initialized
   * @param folioExecutionContext provides the current tenant context
   * @param moduleId identifier of the running module version
   */
  public TenantModuleVersionService(TenantModuleVersionRepository repository,
    TenantInitializationDetector initializationDetector, FolioExecutionContext folioExecutionContext,
    String moduleId) {
    this.repository = repository;
    this.initializationDetector = initializationDetector;
    this.folioExecutionContext = folioExecutionContext;
    this.moduleId = notBlank(moduleId, "moduleId must not be blank");
  }

  /** Records the running module version for the current tenant. */
  public void markCurrentVersion() {
    var tenant = notBlank(folioExecutionContext.getTenantId(), "tenantId must not be blank");
    repository.saveModuleVersion(moduleId);
    tenantsWithMarkerTable.add(tenant);
    log.info("Tenant module version recorded: tenant = {}, moduleId = {}", tenant, moduleId);
  }

  /** Removes the module-version marker for the current tenant when the marker table still exists. */
  public void clear() {
    var tenant = notBlank(folioExecutionContext.getTenantId(), "tenantId must not be blank");
    tenantsWithMarkerTable.remove(tenant);
    if (!repository.markerTableExists()) {
      log.debug("Nothing to clear, tenant module-version table does not exist: tenant = {}", tenant);
      return;
    }

    repository.deleteModuleVersion();
    log.info("Tenant module-version record has been cleared: tenant = {}", tenant);
  }

  /**
   * Returns whether the tenant schema records a different version of this module.
   *
   * <p>An uninitialized tenant, or an initialized tenant whose marker record has not been written
   * yet, is not considered a different version. An initialized schema without the marker table
   * predates marker-based filtering and is considered to use an older version.</p>
   *
   * @return {@code true} when the recorded module version differs from the running version
   */
  public boolean isDifferentModuleVersion() {
    var tenant = notBlank(folioExecutionContext.getTenantId(), "tenantId must not be blank");
    if (tenantsWithMarkerTable.contains(tenant)) {
      try {
        return isStoredModuleVersionDifferent(tenant);
      } catch (InvalidDataAccessResourceUsageException exception) {
        log.debug("Tenant module-version table does not exist anymore: tenant = {}", tenant, exception);
        tenantsWithMarkerTable.remove(tenant);
      }
    }

    return isModuleVersionDifferentForUncachedTenant(tenant);
  }

  private boolean isModuleVersionDifferentForUncachedTenant(String tenant) {
    if (!initializationDetector.isInitialized()) {
      log.debug("Tenant is not initialized yet, no module version is recorded: tenant = {}", tenant);
      return false;
    }

    if (!repository.markerTableExists()) {
      log.debug("Tenant schema predates the module-version marker: tenant = {}", tenant);
      return true;
    }

    tenantsWithMarkerTable.add(tenant);
    return isStoredModuleVersionDifferent(tenant);
  }

  private boolean isStoredModuleVersionDifferent(String tenant) {
    var storedModuleId = repository.findModuleVersion();
    if (storedModuleId.isEmpty()) {
      log.debug("Tenant module-version record is missing: tenant = {}", tenant);
      return false;
    }

    log.debug("Tenant module-version check: tenant = {}, storedModuleId = {}, currentModuleId = {}",
      tenant, storedModuleId.get(), moduleId);
    return !moduleId.equals(storedModuleId.get());
  }
}
