package org.folio.tools.apisix.service;

import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.tools.apisix.service.ApisixRouteFactory.MODULE_LABEL;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.domain.model.error.Parameter;
import org.folio.common.gateway.ApiGatewayService;
import org.folio.common.gateway.exception.TenantRouteUpdateException;
import org.folio.common.gateway.model.GatewayServiceDefinition;
import org.folio.tools.apisix.client.ApisixAdminClient;
import org.folio.tools.apisix.exception.ApisixIntegrationException;
import org.folio.tools.apisix.model.ApisixRoute;
import org.folio.tools.apisix.model.ApisixService;
import org.folio.tools.apisix.model.ApisixTimeout;
import org.folio.tools.apisix.model.ApisixUpstream;
import org.jspecify.annotations.Nullable;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Manages APISIX services and routes for FOLIO modules through the APISIX Admin API.
 */
@Log4j2
@RequiredArgsConstructor
public class ApisixGatewayService implements ApiGatewayService {

  private static final int PAGE_SIZE = 500;
  private static final int MAX_ID_LENGTH = 64;

  private final ApisixAdminClient apisixAdminClient;
  private final ApisixRouteFactory apisixRouteFactory;
  private final ApisixRouteTenantService apisixRouteTenantService;

  @Override
  public void upsertService(GatewayServiceDefinition service) {
    var serviceId = serviceIdOf(service.getName());
    var desired = buildService(serviceId, service);
    try {
      var stored = apisixAdminClient.getService(serviceId).getValue();
      if (sameUpstream(stored, desired)) {
        log.debug("Service already exists in APISIX: moduleId = {}", service.getName());
        return;
      }

      apisixAdminClient.upsertService(serviceId, desired);
      log.debug("Service is updated in APISIX: moduleId = {}", service.getName());
    } catch (HttpClientErrorException.NotFound e) {
      apisixAdminClient.upsertService(serviceId, desired);
      log.debug("Service is created in APISIX: moduleId = {}", service.getName());
    }
  }

  @Override
  public void deleteService(String serviceNameOrId) {
    try {
      apisixAdminClient.deleteService(serviceIdOf(serviceNameOrId));
    } catch (Exception e) {
      log.warn("Failed to delete APISIX service: {}", serviceNameOrId, e);
      var parameters = List.of(new Parameter().key("cause").value(e.getMessage()));
      throw new ApisixIntegrationException("Failed to delete APISIX service: " + serviceNameOrId, parameters);
    }
  }

  @Override
  public void deleteServiceRoutes(String serviceNameOrId) {
    var serviceId = resolveServiceIdForRouteCleanup(serviceNameOrId);
    try {
      for (var route : findRoutes(route -> serviceId.equals(route.getServiceId()))) {
        deleteRouteIgnoringNotFound(route.getId());
      }
    } catch (Exception e) {
      log.warn("Failed to delete all routes for APISIX service: {}", serviceNameOrId, e);
      var parameters = List.of(new Parameter().key("cause").value(e.getMessage()));
      throw new ApisixIntegrationException("Failed to delete all routes for service " + serviceNameOrId,
        parameters, e);
    }
  }

  private String resolveServiceIdForRouteCleanup(String serviceNameOrId) {
    try {
      var stored = apisixAdminClient.getService(serviceIdOf(serviceNameOrId)).getValue();
      return Objects.requireNonNullElse(stored.getId(), serviceIdOf(serviceNameOrId));
    } catch (HttpClientErrorException.NotFound nf) {
      throw new NoSuchElementException("No such service: " + serviceNameOrId);
    }
  }

  private void deleteRouteIgnoringNotFound(String routeId) {
    try {
      apisixAdminClient.deleteRoute(routeId);
    } catch (HttpClientErrorException.NotFound nf) {
      log.debug("Route already absent in APISIX, skipping: routeId = {}", routeId);
    }
  }

  /**
   * Adds routes for API Gateway.
   *
   * @param moduleDescriptors - {@link List} with {@link ModuleDescriptor} objects to be processed
   * @throws ApisixIntegrationException if any of route create requests failed
   */
  @Override
  public void addRoutes(Collection<ModuleDescriptor> moduleDescriptors) {
    performOperation(moduleDescriptors, "create", this::addRoutesForModule);
  }

  /**
   * Updates routes for API Gateway: creates missing routes and deletes stale ones, never re-submitting
   * unchanged existing routes (their per-tenant state is preserved).
   *
   * @param moduleDescriptors - {@link List} with {@link ModuleDescriptor} objects to be processed
   * @throws ApisixIntegrationException if any of route update requests failed
   */
  @Override
  public void updateRoutes(Collection<ModuleDescriptor> moduleDescriptors) {
    performOperation(moduleDescriptors, "update", this::updateRoutesForModule);
  }

  /**
   * Removes routes from API Gateway.
   *
   * @param moduleDescriptors - {@link List} with {@link ModuleDescriptor} objects to be processed
   * @throws ApisixIntegrationException if any of route delete requests failed
   */
  @Override
  public void removeRoutes(Collection<ModuleDescriptor> moduleDescriptors) {
    performOperation(moduleDescriptors, "remove", this::removeRoutesForModule);
  }

  /**
   * Adds a tenant to all routes for a specific module.
   *
   * @param moduleId - the module identifier
   * @param tenantName - the tenant name to add
   * @throws TenantRouteUpdateException if the operation fails
   */
  @Override
  public void addTenantToModuleRoutes(String moduleId, String tenantName) {
    log.info("Adding tenant [{}] to routes for module [{}]", tenantName, moduleId);
    updateTenantOnModuleRoutes(moduleId, tenantName, "add", apisixRouteTenantService::addTenant);
  }

  /**
   * Removes a tenant from all routes for a specific module.
   *
   * @param moduleId - the module identifier
   * @param tenantName - the tenant name to remove
   * @throws TenantRouteUpdateException if the operation fails
   */
  @Override
  public void removeTenantFromModuleRoutes(String moduleId, String tenantName) {
    log.info("Removing tenant [{}] from routes for module [{}]", tenantName, moduleId);
    updateTenantOnModuleRoutes(moduleId, tenantName, "remove", apisixRouteTenantService::removeTenant);
  }

  private void updateTenantOnModuleRoutes(String moduleId, String tenantName, String operation,
    TenantRouteMutator mutator) {
    try {
      validateTenantChangeInput(tenantName, moduleId, operation);
      getExistingServiceId(moduleId);
      var routes = getModuleRoutes(moduleId).stream()
        .filter(route -> isNotEmpty(route.getVars()))
        .toList();
      if (routes.isEmpty()) {
        log.warn("No routes found for module [{}]", moduleId);
        return;
      }

      var failedRoutes = updateTenantOnRoutes(routes, tenantName, mutator);
      validateTenantChangeOperation(tenantName, moduleId, failedRoutes, operation);
      log.info("Successfully processed tenant [{}] for {} routes of module [{}]",
        tenantName, routes.size(), moduleId);
    } catch (TenantRouteUpdateException e) {
      throw e;
    } catch (Exception e) {
      throw new TenantRouteUpdateException(
        "Failed to " + operation + " tenant [" + tenantName + "] to routes for module [" + moduleId + "]",
        List.of(), e);
    }
  }

  private List<String> updateTenantOnRoutes(List<ApisixRoute> routes, String tenantName, TenantRouteMutator mutator) {
    var failedRoutes = new ArrayList<String>();
    for (var route : routes) {
      try {
        if (mutator.apply(route, tenantName)) {
          // PATCH merges only the vars field, so route fields outside this client's model survive the update
          apisixAdminClient.patchRoute(route.getId(), new ApisixRoute().vars(route.getVars()));
        }
      } catch (Exception e) {
        log.error("Failed to update tenant [{}] on route [{}]: {}", tenantName, route.getId(), e.getMessage());
        failedRoutes.add(route.getId());
      }
    }

    return failedRoutes;
  }

  private void performOperation(Collection<ModuleDescriptor> moduleDescriptors, String operation,
    Function<ModuleDescriptor, Collection<Parameter>> moduleOperation) {
    var allErrors = new ArrayList<Parameter>();
    for (var moduleDescriptor : emptyIfNull(moduleDescriptors)) {
      allErrors.addAll(moduleOperation.apply(moduleDescriptor));
    }

    if (isNotEmpty(allErrors)) {
      throw new ApisixIntegrationException(String.format("Failed to %s routes", operation), allErrors);
    }
  }

  private List<Parameter> addRoutesForModule(ModuleDescriptor moduleDescriptor) {
    var serviceId = getExistingServiceId(moduleDescriptor.getId());
    return apisixRouteFactory.createRoutes(moduleDescriptor, serviceId).stream()
      .map(this::upsertRouteSafe)
      .flatMap(Optional::stream)
      .toList();
  }

  private List<Parameter> updateRoutesForModule(ModuleDescriptor moduleDescriptor) {
    var moduleId = moduleDescriptor.getId();
    var serviceId = getExistingServiceId(moduleId);
    var existingRouteIds = getModuleRoutes(moduleId).stream()
      .map(ApisixRoute::getId)
      .collect(toCollection(LinkedHashSet::new));

    var desiredRoutes = apisixRouteFactory.createRoutes(moduleDescriptor, serviceId);
    var errors = new ArrayList<>(createMissingRoutes(desiredRoutes, existingRouteIds));

    var desiredRouteIds = desiredRoutes.stream().map(ApisixRoute::getId).collect(toSet());
    for (var existingId : existingRouteIds) {
      if (!desiredRouteIds.contains(existingId)) {
        deleteRouteSafe(existingId).ifPresent(errors::add);
      }
    }

    return errors;
  }

  private List<Parameter> createMissingRoutes(List<ApisixRoute> desiredRoutes, LinkedHashSet<String> existingIds) {
    return desiredRoutes.stream()
      .filter(route -> !existingIds.contains(route.getId()))
      .map(this::upsertRouteSafe)
      .flatMap(Optional::stream)
      .toList();
  }

  private List<Parameter> removeRoutesForModule(ModuleDescriptor moduleDescriptor) {
    var moduleId = moduleDescriptor.getId();
    getExistingServiceId(moduleId);
    return getModuleRoutes(moduleId).stream()
      .map(route -> deleteRouteSafe(route.getId()))
      .flatMap(Optional::stream)
      .toList();
  }

  private Optional<Parameter> upsertRouteSafe(ApisixRoute route) {
    try {
      apisixAdminClient.upsertRoute(route.getId(), route);
      return Optional.empty();
    } catch (RestClientException exception) {
      return Optional.of(new Parameter().key(route.getId()).value(exception.getMessage()));
    }
  }

  private Optional<Parameter> deleteRouteSafe(String routeId) {
    try {
      apisixAdminClient.deleteRoute(routeId);
      return Optional.empty();
    } catch (Exception exception) {
      return Optional.of(new Parameter().key(routeId).value(exception.getMessage()));
    }
  }

  private String getExistingServiceId(String moduleId) {
    try {
      var stored = apisixAdminClient.getService(serviceIdOf(moduleId)).getValue();
      return Objects.requireNonNullElse(stored.getId(), serviceIdOf(moduleId));
    } catch (HttpClientErrorException.NotFound exception) {
      var parameter = new Parameter().key(moduleId).value("Service is not found");
      throw new ApisixIntegrationException("Failed to find APISIX service for module: " + moduleId,
        List.of(parameter));
    } catch (Exception exception) {
      var parameter = new Parameter().key(moduleId).value(exception.getMessage());
      throw new ApisixIntegrationException("Failed to find APISIX service for module: " + moduleId,
        List.of(parameter), exception);
    }
  }

  private List<ApisixRoute> getModuleRoutes(String moduleId) {
    return findRoutes(route -> route.getLabels() != null && moduleId.equals(route.getLabels().get(MODULE_LABEL)));
  }

  private List<ApisixRoute> findRoutes(Predicate<ApisixRoute> predicate) {
    var result = new ArrayList<ApisixRoute>();
    var page = 1;
    var fetched = 0;
    Integer total;
    int pageEntries;
    do {
      var routePage = fetchRoutePage(page);
      var entries = emptyIfNull(routePage.getList());
      pageEntries = entries.size();
      fetched += pageEntries;
      total = routePage.getTotal();
      entries.stream()
        .map(ApisixAdminClient.ApisixEntry::getValue)
        .filter(Objects::nonNull)
        .filter(predicate)
        .forEach(result::add);
      page++;
    } while (pageEntries == PAGE_SIZE && isBelowTotal(fetched, total));

    return result;
  }

  private ApisixAdminClient.ApisixResultList<ApisixRoute> fetchRoutePage(int page) {
    try {
      return apisixAdminClient.getRoutes(page, PAGE_SIZE);
    } catch (Exception exception) {
      var parameter = new Parameter().key("Failed to find routes").value(exception.getMessage());
      throw new ApisixIntegrationException("Failed to load routes", List.of(parameter), exception);
    }
  }

  private static boolean isBelowTotal(int fetched, @Nullable Integer total) {
    return total == null || fetched < total;
  }

  private static ApisixService buildService(String serviceId, GatewayServiceDefinition definition) {
    var uri = URI.create(definition.getUrl());
    validateUpstreamUrl(uri, definition);
    var scheme = uri.getScheme() != null ? uri.getScheme() : "http";
    var port = uri.getPort() != -1 ? uri.getPort() : ("https".equals(scheme) ? 443 : 80);
    var upstream = new ApisixUpstream()
      .type("roundrobin")
      .scheme(scheme)
      .nodes(Map.of(uri.getHost() + ":" + port, 1))
      .passHost("pass")
      .retries(definition.getRetries())
      .timeout(buildTimeout(definition));
    return new ApisixService().id(serviceId).name(definition.getName())
      .plugins(buildUpstreamPathPlugins(uri))
      .upstream(upstream);
  }

  // java.net.URI returns a null host for registry-based authorities (e.g. underscores in docker-compose
  // container names) and scheme-less strings — fail fast instead of registering a broken "null:80" upstream.
  private static void validateUpstreamUrl(URI uri, GatewayServiceDefinition definition) {
    if (uri.getHost() == null) {
      throw new ApisixIntegrationException("Invalid upstream URL for service " + definition.getName()
        + ": host cannot be resolved from the URL",
        List.of(new Parameter().key(definition.getName()).value(definition.getUrl())));
    }
  }

  // APISIX upstream nodes cannot carry a URL path the way Kong service urls do; a path-carrying discovery
  // URL is mapped to a service-level proxy-rewrite prefixing the upstream path.
  private static @Nullable Map<String, Object> buildUpstreamPathPlugins(URI uri) {
    var path = uri.getPath();
    if (!isNotBlank(path) || "/".equals(path)) {
      return null;
    }

    var prefix = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    return Map.of("proxy-rewrite", Map.of("regex_uri", List.of("^(.*)", prefix + "$1")));
  }

  /**
   * APISIX requires all three timeout fields when the {@code timeout} object is present; unset values fall back
   * to the APISIX default of 60 seconds. When no timeout is configured, the object is omitted entirely.
   */
  private static @Nullable ApisixTimeout buildTimeout(GatewayServiceDefinition definition) {
    if (definition.getConnectTimeout() == null && definition.getReadTimeout() == null
      && definition.getWriteTimeout() == null) {
      return null;
    }

    return new ApisixTimeout()
      .connect(toSeconds(definition.getConnectTimeout()))
      .read(toSeconds(definition.getReadTimeout()))
      .send(toSeconds(definition.getWriteTimeout()));
  }

  private static Double toSeconds(@Nullable Integer milliseconds) {
    return milliseconds != null ? milliseconds / 1000.0 : 60.0;
  }

  private static boolean sameUpstream(ApisixService stored, ApisixService desired) {
    var storedUpstream = stored.getUpstream();
    var desiredUpstream = desired.getUpstream();
    return storedUpstream != null
      && Objects.equals(storedUpstream.getScheme(), desiredUpstream.getScheme())
      && Objects.equals(storedUpstream.getNodes(), desiredUpstream.getNodes())
      && Objects.equals(stored.getPlugins(), desired.getPlugins());
  }

  private static String serviceIdOf(String name) {
    return name.length() <= MAX_ID_LENGTH ? name : sha1Hex(name);
  }

  private static void validateTenantChangeOperation(String tenantName, String moduleId, List<String> failedRoutes,
    String operation) {
    if (!failedRoutes.isEmpty()) {
      var errors = mapItems(failedRoutes,
        routeId -> new Parameter().key(routeId).value("Failed to " + operation + " tenant"));
      throw new TenantRouteUpdateException(
        "Failed to " + operation + " tenant [" + tenantName + "] to routes for module [" + moduleId + "]. "
          + "Failed routes: " + String.join(", ", failedRoutes), errors);
    }
  }

  private static void validateTenantChangeInput(String tenantName, String moduleId, String operation) {
    if (isBlank(tenantName) || isBlank(moduleId)) {
      throw new IllegalStateException(
        "Failed to " + operation + " tenant [" + tenantName + "] to routes for module [" + moduleId + "]."
          + "Tenant name and module id must be non-blank strings.");
    }
  }

  private interface TenantRouteMutator {
    boolean apply(ApisixRoute route, String tenantName);
  }
}
