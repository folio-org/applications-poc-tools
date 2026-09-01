package org.folio.common.gateway;

import java.util.Collection;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.gateway.model.GatewayServiceDefinition;

/**
 * Gateway-agnostic contract for managing API Gateway services and routes for FOLIO modules.
 *
 * <p>Implementations translate FOLIO module descriptors into the gateway's native service/route model.</p>
 */
public interface ApiGatewayService {

  /**
   * Creates or updates the gateway service (upstream) for a module.
   *
   * <p>Must be a no-op when the stored upstream URL already matches the definition.</p>
   *
   * @param service - gateway-neutral service definition
   */
  void upsertService(GatewayServiceDefinition service);

  /**
   * Deletes the gateway service by name or id.
   *
   * @param serviceNameOrId - service name or identifier
   * @throws org.folio.common.gateway.exception.ApiGatewayIntegrationException if the operation fails
   */
  void deleteService(String serviceNameOrId);

  /**
   * Deletes all routes of a gateway service.
   *
   * @param serviceNameOrId - service name or identifier
   * @throws java.util.NoSuchElementException if the service does not exist
   * @throws org.folio.common.gateway.exception.ApiGatewayIntegrationException on other failures
   */
  void deleteServiceRoutes(String serviceNameOrId);

  /**
   * Adds routes for the given module descriptors. The service for each module must already exist.
   *
   * <p>Per-item failures are collected and reported in a single exception; the operation does not fail fast.</p>
   *
   * @param moduleDescriptors - module descriptors to process
   * @throws org.folio.common.gateway.exception.ApiGatewayIntegrationException aggregating per-item failures
   */
  void addRoutes(Collection<ModuleDescriptor> moduleDescriptors);

  /**
   * Updates routes for the given module descriptors by diffing desired against existing routes:
   * missing routes are created, stale routes are deleted, and unchanged existing routes are never
   * re-submitted (preserving per-tenant route state).
   *
   * @param moduleDescriptors - module descriptors to process
   * @throws org.folio.common.gateway.exception.ApiGatewayIntegrationException aggregating per-item failures
   */
  void updateRoutes(Collection<ModuleDescriptor> moduleDescriptors);

  /**
   * Removes all routes of the given module descriptors.
   *
   * @param moduleDescriptors - module descriptors to process
   * @throws org.folio.common.gateway.exception.ApiGatewayIntegrationException aggregating per-item failures
   */
  void removeRoutes(Collection<ModuleDescriptor> moduleDescriptors);

  /**
   * Adds a tenant to all routes of a module. Idempotent: adding an already-present tenant is a no-op;
   * a module without routes results in a warning, not an error.
   *
   * @param moduleId - the module identifier
   * @param tenantName - the tenant name to add
   * @throws org.folio.common.gateway.exception.TenantRouteUpdateException if the operation fails
   */
  void addTenantToModuleRoutes(String moduleId, String tenantName);

  /**
   * Removes a tenant from all routes of a module. Idempotent: removing an absent tenant is a no-op;
   * a module without routes results in a warning, not an error.
   *
   * @param moduleId - the module identifier
   * @param tenantName - the tenant name to remove
   * @throws org.folio.common.gateway.exception.TenantRouteUpdateException if the operation fails
   */
  void removeTenantFromModuleRoutes(String moduleId, String tenantName);
}
