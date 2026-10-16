package org.folio.integration.kafka.consumer.filter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

/**
 * Records which module version initialized a tenant and resolves whether a Kafka event belongs to
 * another version of the module.
 */
@Log4j2
public class TenantModuleVersionService {

  private final TenantModuleVersionRepository repository;
  private final TenantInitializationDetector initializationDetector;
  private final Supplier<String> tenantIdSupplier;
  private final UnaryOperator<String> tenantSchemaResolver;
  private final String moduleId;
  private final Set<String> tenantsWithMarkerTable = ConcurrentHashMap.newKeySet();

  /**
   * Creates a tenant module-version service.
   *
   * @param repository stores and retrieves module-version markers
   * @param initializationDetector determines whether a tenant schema is initialized
   * @param tenantIdSupplier supplies the tenant from the current execution context
   * @param tenantSchemaResolver resolves a tenant name to its database schema
   * @param moduleId identifier of the running module version
   */
  public TenantModuleVersionService(TenantModuleVersionRepository repository,
    TenantInitializationDetector initializationDetector, Supplier<String> tenantIdSupplier,
    UnaryOperator<String> tenantSchemaResolver, String moduleId) {
    this.repository = repository;
    this.initializationDetector = initializationDetector;
    this.tenantIdSupplier = tenantIdSupplier;
    this.tenantSchemaResolver = tenantSchemaResolver;
    this.moduleId = requireNonBlank(moduleId, "moduleId");
  }

  /** Records the running module version for the current tenant. */
  public void markCurrentVersion() {
    var tenant = requireNonBlank(tenantIdSupplier.get(), "tenantId");
    var schema = resolveSchema(tenant);
    repository.saveModuleVersion(schema, moduleId);
    tenantsWithMarkerTable.add(tenant);
    log.info("Tenant module version recorded: tenant = {}, moduleId = {}", tenant, moduleId);
  }

  /** Removes the module-version marker for the current tenant when the marker table still exists. */
  public void clear() {
    var tenant = requireNonBlank(tenantIdSupplier.get(), "tenantId");
    tenantsWithMarkerTable.remove(tenant);
    var schema = resolveSchema(tenant);
    if (!repository.markerTableExists(schema)) {
      log.debug("Nothing to clear, tenant module-version table does not exist: tenant = {}", tenant);
      return;
    }

    repository.deleteModuleVersion(schema);
    log.info("Tenant module-version record has been cleared: tenant = {}", tenant);
  }

  /**
   * Returns whether the tenant schema records a different version of this module.
   *
   * <p>An uninitialized tenant, or an initialized tenant whose marker record has not been written
   * yet, is not considered a different version. An initialized schema without the marker table
   * predates marker-based filtering and is considered to use an older version.</p>
   *
   * @param tenant tenant identifier
   * @return {@code true} when the recorded module version differs from the running version
   */
  public boolean isDifferentModuleVersion(String tenant) {
    var validatedTenant = requireNonBlank(tenant, "tenant");
    var schema = resolveSchema(validatedTenant);
    if (tenantsWithMarkerTable.contains(validatedTenant)) {
      try {
        return isStoredModuleVersionDifferent(validatedTenant, schema);
      } catch (InvalidDataAccessResourceUsageException exception) {
        log.debug("Tenant module-version table does not exist anymore: tenant = {}", validatedTenant, exception);
        tenantsWithMarkerTable.remove(validatedTenant);
      }
    }

    return resolveModuleVersionDifference(validatedTenant, schema);
  }

  private boolean resolveModuleVersionDifference(String tenant, String schema) {
    if (!initializationDetector.isInitialized(schema)) {
      log.debug("Tenant is not initialized yet, no module version is recorded: tenant = {}", tenant);
      return false;
    }

    if (!repository.markerTableExists(schema)) {
      log.debug("Tenant schema predates the module-version marker: tenant = {}", tenant);
      return true;
    }

    tenantsWithMarkerTable.add(tenant);
    return isStoredModuleVersionDifferent(tenant, schema);
  }

  private boolean isStoredModuleVersionDifferent(String tenant, String schema) {
    var storedModuleId = repository.findModuleVersion(schema);
    if (storedModuleId.isEmpty()) {
      log.debug("Tenant module-version record is missing: tenant = {}", tenant);
      return false;
    }

    log.debug("Tenant module-version check: tenant = {}, storedModuleId = {}, currentModuleId = {}",
      tenant, storedModuleId.get(), moduleId);
    return !moduleId.equals(storedModuleId.get());
  }

  private String resolveSchema(String tenant) {
    return requireNonBlank(tenantSchemaResolver.apply(tenant), "schemaName");
  }

  private static String requireNonBlank(String value, String name) {
    if (isBlank(value)) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value;
  }
}
