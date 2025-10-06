package org.folio.tools.kong.service;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.logging.log4j.util.Strings.isBlank;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.common.utils.OkapiHeaders.MODULE_ID;
import static org.folio.tools.kong.model.expression.RouteExpressions.combineUsingAnd;
import static org.folio.tools.kong.model.expression.RouteExpressions.combineUsingOr;
import static org.folio.tools.kong.model.expression.RouteExpressions.httpHeader;
import static org.folio.tools.kong.model.expression.RouteExpressions.httpMethod;
import static org.folio.tools.kong.model.expression.RouteExpressions.httpPath;
import static org.folio.tools.kong.utls.RoutingEntryUtils.getMethods;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

import feign.FeignException;
import feign.FeignException.NotFound;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.common.domain.model.InterfaceDescriptor;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.common.domain.model.error.Parameter;
import org.folio.tools.kong.client.KongAdminClient;
import org.folio.tools.kong.client.KongAdminClient.KongResultList;
import org.folio.tools.kong.exception.KongIntegrationException;
import org.folio.tools.kong.exception.TenantRouteUpdateException;
import org.folio.tools.kong.model.Route;
import org.folio.tools.kong.model.Service;
import org.folio.tools.kong.model.expression.RouteExpression;
import org.jetbrains.annotations.Nullable;

@Log4j2
@RequiredArgsConstructor
public class KongGatewayService {

  public static final String MULTIPLE_INTERFACE_TYPE = "multiple";
  private static final String KONG_PATH_VARIABLE_REGEX_GROUP = "([^/]+)";
  private static final Pattern PATH_VARIABLE_REGEX = Pattern.compile("\\{[^}]+}");

  private final KongAdminClient kongAdminClient;
  private final KongRouteTenantService kongRouteTenantService;

  /**
   * Adds routes for API Gateway.
   *
   * @param moduleDescriptors - {@link List} with {@link ModuleDescriptor} objects to be processed
   * @throws KongIntegrationException if any of route create requests failed
   */
  public void addRoutes(Collection<ModuleDescriptor> moduleDescriptors) {
    performOperation(moduleDescriptors, "create", this::addRoutesForModule);
  }

  /**
   * Updates routes for API Gateway.
   *
   * @param moduleDescriptors - {@link List} with {@link ModuleDescriptor} objects to be processed
   * @throws KongIntegrationException if any of route update requests failed
   */
  public void updateRoutes(Collection<ModuleDescriptor> moduleDescriptors) {
    performOperation(moduleDescriptors, "update", this::updateRoutesForModule);
  }

  /**
   * Removes routes from API Gateway.
   *
   * @param moduleDescriptors - {@link List} with {@link ModuleDescriptor} objects to be processed
   * @throws KongIntegrationException if any of route delete requests failed
   */
  public void removeRoutes(Collection<ModuleDescriptor> moduleDescriptors) {
    performOperation(moduleDescriptors, "remove", md -> removeKongRoutes(md.getId()));
  }

  /**
   * Adds a tenant to all routes for a specific module.
   *
   * @param moduleId - the module identifier
   * @param tenantName - the tenant name to add
   * @throws TenantRouteUpdateException if the operation fails
   */
  public void addTenantToModuleRoutes(String moduleId, String tenantName) {
    log.info("Adding tenant [{}] to routes for module [{}]", tenantName, moduleId);
    try {
      validateTenantChangeInput(tenantName, moduleId, "add");
      var serviceId = getExistingServiceId(moduleId);
      var routes = toStream(getKongRoutes(moduleId))
        .filter(Objects::nonNull)
        .filter(route -> isNotBlank(route.getExpression()))
        .toList();
      
      if (routes.isEmpty()) {
        log.warn("No routes found for module [{}]", moduleId);
        return;
      }

      var failedRoutes = addTenantToRoutes(routes, tenantName, serviceId);
      validateTenantChangeOperation(tenantName, moduleId, failedRoutes, "add");

      log.info("Successfully added tenant [{}] to {} routes for module [{}]", tenantName, routes.size(), moduleId);
    } catch (TenantRouteUpdateException e) {
      throw e;
    } catch (Exception e) {
      throw new TenantRouteUpdateException(
        "Failed to add tenant [" + tenantName + "] to routes for module [" + moduleId + "]", e);
    }
  }

  /**
   * Removes a tenant from all routes for a specific module.
   *
   * @param moduleId - the module identifier
   * @param tenantName - the tenant name to remove
   * @throws TenantRouteUpdateException if the operation fails
   */
  public void removeTenantFromModuleRoutes(String moduleId, String tenantName) {
    log.info("Removing tenant [{}] from routes for module [{}]", tenantName, moduleId);
    try {
      validateTenantChangeInput(tenantName, moduleId, "remove");
      var serviceId = getExistingServiceId(moduleId);
      var routes = toStream(getKongRoutes(moduleId))
        .filter(Objects::nonNull)
        .filter(route -> isNotBlank(route.getExpression()))
        .toList();
      
      if (routes.isEmpty()) {
        log.warn("No routes found for module [{}]", moduleId);
        return;
      }

      var failedRoutes = removeTenantFromRoutes(routes, tenantName, serviceId);
      validateTenantChangeOperation(tenantName, moduleId, failedRoutes, "remove");

      log.info("Successfully removed tenant [{}] from {} routes for module [{}]", tenantName, routes.size(), moduleId);
    } catch (TenantRouteUpdateException e) {
      throw e;
    } catch (Exception e) {
      throw new TenantRouteUpdateException(
        "Failed to remove tenant [" + tenantName + "] from routes for module [" + moduleId + "]", e);
    }
  }

  private List<String> addTenantToRoutes(List<Route> routes, String tenantName, String serviceId) {
    var failedRoutes = new ArrayList<String>();
    for (var route : routes) {
      try {
        var changedRoute = kongRouteTenantService.addTenant(route, tenantName);
        kongAdminClient.upsertRoute(serviceId, route.getName(), changedRoute);
        log.debug("Successfully added tenant [{}] to route [{}]", tenantName, route.getName());
      } catch (Exception e) {
        log.error("Failed to add tenant [{}] to route [{}]: {}", tenantName, route.getName(), e.getMessage());
        failedRoutes.add(route.getName());
      }
    }
    return failedRoutes;
  }

  private List<String> removeTenantFromRoutes(List<Route> routes, String tenantName, String serviceId) {
    var failedRoutes = new ArrayList<String>();
    for (var route : routes) {
      try {
        var changedRoute = kongRouteTenantService.removeTenant(route, tenantName);
        kongAdminClient.upsertRoute(serviceId, route.getName(), changedRoute);
        log.debug("Successfully removed tenant [{}] from route [{}]", tenantName, route.getName());
      } catch (Exception e) {
        log.error("Failed to remove tenant [{}] from route [{}]: {}", tenantName, route.getName(), e.getMessage());
        failedRoutes.add(route.getName());
      }
    }
    return failedRoutes;
  }

  /**
   * Creates a new {@link Service} in Kong if it does not exist.
   *
   * @param service - {@link Service} to be created
   */
  public void upsertService(Service service) {
    var serviceName = service.getName();
    try {
      var kongService = kongAdminClient.getService(serviceName);
      var kongUrl = getUrl(kongService);
      if (!Objects.equals(kongUrl, service.getUrl())) {
        log.debug("Service is updated in Kong: moduleId = {}", serviceName);
        kongAdminClient.upsertService(serviceName, service);
      } else {
        log.debug("Service already exists in Kong: moduleId = {}", serviceName);
      }
    } catch (FeignException.NotFound e) {
      kongAdminClient.upsertService(serviceName, service);
      log.debug("Service is created in Kong: moduleId = {}", serviceName);
    }
  }

  /**
   * Deletes Kong service by service name or id.
   *
   * @throws KongIntegrationException - if integration exception happened
   */
  public void deleteService(String serviceNameOrId) {
    try {
      kongAdminClient.deleteService(serviceNameOrId);
    } catch (Exception e) {
      log.warn("Failed to delete Kong service: {}", serviceNameOrId, e);
      var parameters = List.of(new Parameter().key("cause").value(e.getMessage()));
      throw new KongIntegrationException("Failed to delete Kong service: " + serviceNameOrId, parameters);
    }
  }

  public void deleteServiceRoutes(String serviceNameOrId) {
    try {
      KongResultList<Route> routes;
      do {
        routes = kongAdminClient.getServiceRoutes(serviceNameOrId, null);
        routes.forEach(route -> kongAdminClient.deleteRoute(serviceNameOrId, route.getId()));
      } while (routes.getData() != null && !routes.getData().isEmpty());
    } catch (NotFound nf) {
      throw new NoSuchElementException("No such service: " + serviceNameOrId);
    } catch (Exception e) {
      log.warn("Failed to delete all routes for Kong service: {}", serviceNameOrId, e);
      var parameters = List.of(new Parameter().key("cause").value(e.getMessage()));
      throw new KongIntegrationException("Failed to delete all routes for service " + serviceNameOrId, parameters, e);
    }
  }

  private String getUrl(Service service) {
    return service.getUrl() != null
      ? service.getUrl()
      : String.format("%s://%s:%s", service.getProtocol(), service.getHost(), service.getPort());
  }

  private void performOperation(Collection<ModuleDescriptor> moduleDescriptors, String operation,
    Function<ModuleDescriptor, Collection<Parameter>> moduleOperation) {
    var allErrors = new ArrayList<Parameter>();
    for (var moduleDescriptor : emptyIfNull(moduleDescriptors)) {
      allErrors.addAll(moduleOperation.apply(moduleDescriptor));
    }

    if (isNotEmpty(allErrors)) {
      throw new KongIntegrationException(String.format("Failed to %s routes", operation), allErrors);
    }
  }

  private List<Parameter> updateRoutesForModule(ModuleDescriptor moduleDescriptor) {
    var moduleId = moduleDescriptor.getId();
    var serviceId = getExistingServiceId(moduleId);
    var existingRouteNames = getKongRoutes(moduleId).stream()
      .map(Route::getName)
      .collect(toCollection(LinkedHashSet::new));

    var routes = prepareRoutes(moduleDescriptor, moduleId);
    var newRoutesCreationErrors = createNewKongRoutes(routes, existingRouteNames, serviceId);

    var routeNames = routes.stream()
      .map(pair -> pair.getLeft().getName())
      .collect(toSet());

    var deprecatedRoutesDeletionErrors = deleteDeprecatedKongRoutes(existingRouteNames, routeNames, serviceId);

    var resultErrorParameters = new ArrayList<>(newRoutesCreationErrors);
    resultErrorParameters.addAll(deprecatedRoutesDeletionErrors);

    return resultErrorParameters;
  }

  private List<Parameter> createNewKongRoutes(List<Pair<Route, RoutingEntry>> routes,
    LinkedHashSet<String> existingRouteNames, String serviceId) {
    return toStream(routes)
      .filter(not(pair -> existingRouteNames.contains(pair.getLeft().getName())))
      .map(pair -> createKongRoute(serviceId, pair.getLeft(), pair.getRight()))
      .flatMap(Optional::stream)
      .toList();
  }

  private List<Parameter> deleteDeprecatedKongRoutes(LinkedHashSet<String> existingRouteNames, Set<String> routeNames,
    String serviceId) {
    return toStream(existingRouteNames)
      .filter(not(routeNames::contains))
      .map(routeName -> deleteRoute(serviceId, routeName))
      .flatMap(Optional::stream)
      .toList();
  }

  private List<Parameter> addRoutesForModule(ModuleDescriptor moduleDescriptor) {
    var moduleId = moduleDescriptor.getId();
    var serviceId = getExistingServiceId(moduleId);
    return prepareRoutes(moduleDescriptor, moduleId).stream()
      .map(kongRoutePair -> createKongRoute(serviceId, kongRoutePair.getLeft(), kongRoutePair.getRight()))
      .flatMap(Optional::stream)
      .toList();
  }

  private List<Pair<Route, RoutingEntry>> prepareRoutes(ModuleDescriptor desc, String moduleId) {
    return toStream(desc.getProvides())
      .map(interfaceDescriptor -> prepareRoutes(interfaceDescriptor, moduleId, desc.isMgrComponent()))
      .flatMap(Collection::stream)
      .toList();
  }

  private List<Pair<Route, RoutingEntry>> prepareRoutes(InterfaceDescriptor desc, String moduleId,
    boolean isMgrComponent) {
    var interfaceId = desc.getId() + "-" + desc.getVersion();
    if (desc.isSystem()) {
      log.debug("System interface is ignored: moduleId={}, interfaceId={}]", moduleId, interfaceId);
      return emptyList();
    }

    var isMultiple = nullSafeEquals(desc.getInterfaceType(), MULTIPLE_INTERFACE_TYPE);
    return toStream(desc.getHandlers())
      .map(routingEntry -> getRoute(moduleId, interfaceId, routingEntry, isMgrComponent, isMultiple)
        .map(route -> Pair.of(route, routingEntry)))
      .flatMap(Optional::stream)
      .toList();
  }

  private String getExistingServiceId(String moduleId) {
    try {
      return kongAdminClient.getService(moduleId).getId();
    } catch (Exception exception) {
      var parameter = new Parameter().key(moduleId).value("Service is not found");
      throw new KongIntegrationException("Failed to find Kong service for module: " + moduleId, List.of(parameter));
    }
  }

  private Optional<Parameter> createKongRoute(String serviceId, Route route, RoutingEntry re) {
    try {
      kongAdminClient.upsertRoute(serviceId, route.getName(), route);
      return Optional.empty();
    } catch (FeignException exception) {
      return Optional.of(new Parameter().key(asString(re)).value(exception.getMessage()));
    }
  }

  private List<Route> getKongRoutes(String moduleId) {
    var routes = new ArrayList<Route>();
    var errorParameters = new ArrayList<Parameter>();
    String offset = null;
    do {
      try {
        var routePage = kongAdminClient.getRoutesByTag(getTagsToSearch(moduleId), offset);
        routes.addAll(routePage.getData());
        offset = routePage.getOffset();
      } catch (Exception exception) {
        errorParameters.add(new Parameter().key("Failed to find routes").value(exception.getMessage()));
        offset = null;
      }
    } while (offset != null);

    if (isNotEmpty(errorParameters)) {
      throw new KongIntegrationException("Failed to load routes", errorParameters);
    }

    return routes;
  }

  private List<Parameter> removeKongRoutes(String moduleId) {
    var serviceId = getExistingServiceId(moduleId);
    return getKongRoutes(moduleId).stream()
      .map(kongRoute -> deleteRoute(serviceId, kongRoute.getId()))
      .flatMap(Optional::stream)
      .toList();
  }

  private Optional<Parameter> deleteRoute(String serviceId, String routeIdOrName) {
    try {
      kongAdminClient.deleteRoute(serviceId, routeIdOrName);
      return Optional.empty();
    } catch (Exception exception) {
      return Optional.of(new Parameter().key(routeIdOrName).value(exception.getMessage()));
    }
  }

  @SuppressWarnings("java:S4790")
  private static Optional<Route> getRoute(String moduleId, String interfaceId, RoutingEntry re,
    boolean isMgrComponent, boolean isMultiple) {

    var staticPath = re.getStaticPath();
    var httpMethods = getMethods(re);
    if (StringUtils.isEmpty(staticPath) || CollectionUtils.isEmpty(httpMethods)) {
      log.debug("Route cannot be created: moduleId={}, interfaceId={}, routingEntry={}",
        () -> moduleId, () -> interfaceId, () -> asString(re));
      return Optional.empty();
    }

    var kongPathPair = updatePathPatternForKongGateway(staticPath);
    var path = kongPathPair.getLeft();

    var routeName = buildRouteName(moduleId, interfaceId, path, httpMethods);

    var pathExpression = path.endsWith("$") ? httpPath().regexMatching(path) : httpPath().equalsTo(path);
    var methodsExpression = combineUsingOr(mapItems(httpMethods, method -> httpMethod().equalsTo(method)));
    var moduleIdExp = buildModuleIdHeaderExpression(moduleId, isMultiple);
    var tenantExp = buildTenantHeaderExpression(isMgrComponent);
    return Optional.of(new Route()
      .priority(kongPathPair.getRight())
      .name(sha1Hex(routeName))
      .expression(combineUsingAnd(getNonNullValues(pathExpression, methodsExpression, moduleIdExp, tenantExp)))
      .tags(getNonNullValues(moduleId, interfaceId))
      .stripPath(false)
    );
  }

  private static String buildRouteName(String moduleId, String interfaceId, String path, List<String> httpMethods) {
    return Stream.of(path, String.join(",", httpMethods), moduleId, interfaceId)
      .filter(StringUtils::isNotBlank)
      .collect(joining("|"));
  }

  /**
   * Kong starting from version 3 handles request using expressions, but doing it without an exact match on regex, so
   * each pattern should start with '^' symbol.
   *
   * @param staticPath - request path
   * @return pair of updated path and its priority
   */
  private static Pair<String, Integer> updatePathPatternForKongGateway(String staticPath) {
    if (StringUtils.containsAny(staticPath, '{', '}', '*')) {
      var pathRegex = PATH_VARIABLE_REGEX.matcher(staticPath)
        .replaceAll(KONG_PATH_VARIABLE_REGEX_GROUP)
        .replace("*", "(.*)")
        + "$";
      return Pair.of("^" + pathRegex, 0);
    }

    return Pair.of(staticPath, 1);
  }

  private static String asString(Object re) {
    return re.toString().replace("\n", "\\n");
  }

  private static String getTagsToSearch(String... values) {
    return Arrays.stream(values)
      .filter(Objects::nonNull)
      .collect(joining(","));
  }

  @SafeVarargs
  private static <T> List<T> getNonNullValues(T... nullableValues) {
    return Stream.of(nullableValues).filter(Objects::nonNull).toList();
  }

  private static RouteExpression buildModuleIdHeaderExpression(String moduleId, boolean isMultiple) {
    return isMultiple ? httpHeader(MODULE_ID).equalsTo(moduleId) : null;
  }

  private static @Nullable RouteExpression buildTenantHeaderExpression(boolean isMgrComponent) {
    return isMgrComponent ? null : httpHeader("x-okapi-tenant").headerRegexMatching("\".*\"");
  }

  private static void validateTenantChangeOperation(String tenantName, String moduleId, List<String> failedRoutes,
    String operation) {
    if (!failedRoutes.isEmpty()) {
      throw new TenantRouteUpdateException(
        "Failed to " + operation + " tenant [" + tenantName + "] to routes for module [" + moduleId + "]. "
          + "Failed routes: " + String.join(", ", failedRoutes));
    }
  }

  private static void validateTenantChangeInput(String tenantName, String moduleId, String operation) {
    if (isBlank(tenantName) || isBlank(moduleId)) {
      throw new IllegalStateException(
        "Failed to " + operation + " tenant [" + tenantName + "] to routes for module [" + moduleId + "]."
          + "Tenant name and module id must be non-blank strings.");
    }
  }
}
