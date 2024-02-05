package org.folio.tools.kong.service;

import static java.util.Collections.emptyList;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.common.utils.OkapiHeaders.MODULE_ID;
import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.folio.tools.kong.model.expression.RouteExpressions.combineUsingAnd;
import static org.folio.tools.kong.model.expression.RouteExpressions.combineUsingOr;
import static org.folio.tools.kong.model.expression.RouteExpressions.httpHeader;
import static org.folio.tools.kong.model.expression.RouteExpressions.httpMethod;
import static org.folio.tools.kong.model.expression.RouteExpressions.httpPath;
import static org.folio.tools.kong.utls.RoutingEntryUtils.getMethods;

import feign.FeignException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
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
import org.folio.tools.kong.exception.KongIntegrationException;
import org.folio.tools.kong.model.Route;
import org.folio.tools.kong.model.Service;
import org.folio.tools.kong.model.expression.RouteExpression;

@Log4j2
@RequiredArgsConstructor
public class KongGatewayService {

  private static final String MULTIPLE_INTERFACE_TYPE = "multiple";
  private static final String KONG_PATH_VARIABLE_REGEX_GROUP = "([^/]+)";
  private static final Pattern PATH_VARIABLE_REGEX = Pattern.compile("\\{[^}]+}");

  private final KongAdminClient kongAdminClient;

  /**
   * Adds tenant routes for API Gateway.
   *
   * @param tenant - tenant name as {@link String}, nullable
   * @param moduleDescriptors - {@link List} with {@link ModuleDescriptor} objects to be processed
   * @throws KongIntegrationException if any of route create requests failed
   */
  public void addRoutes(String tenant, Collection<ModuleDescriptor> moduleDescriptors) {
    performOperation(tenant, moduleDescriptors, "create", (t, md) -> addRoutesForModule(md, t));
  }

  /**
   * Updates tenant routes for API Gateway.
   *
   * @param tenant - tenant name as {@link String}, nullable
   * @param moduleDescriptors - {@link List} with {@link ModuleDescriptor} objects to be processed
   * @throws KongIntegrationException if any of route update requests failed
   */
  public void updateRoutes(String tenant, Collection<ModuleDescriptor> moduleDescriptors) {
    performOperation(tenant, moduleDescriptors, "update", (t, md) -> updateRoutesForModule(md, t));
  }

  /**
   * Removes routes for tenant from API Gateway.
   *
   * @param tenant - tenant name as {@link String}, nullable
   * @param moduleDescriptors - {@link List} with {@link ModuleDescriptor} objects to be processed
   * @throws KongIntegrationException if any of route delete requests failed
   */
  public void removeRoutes(String tenant, Collection<ModuleDescriptor> moduleDescriptors) {
    performOperation(tenant, moduleDescriptors, "remove", (t, md) -> removeKongRoutes(t, md.getId()));
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

  private String getUrl(Service service) {
    return service.getUrl() != null
      ? service.getUrl()
      : String.format("%s://%s:%s", service.getProtocol(), service.getHost(), service.getPort());
  }

  private void performOperation(String tenant, Collection<ModuleDescriptor> moduleDescriptors, String operation,
    BiFunction<String, ModuleDescriptor, Collection<Parameter>> moduleOperation) {
    var allErrors = new ArrayList<Parameter>();
    for (var moduleDescriptor : emptyIfNull(moduleDescriptors)) {
      allErrors.addAll(moduleOperation.apply(tenant, moduleDescriptor));
    }

    if (isNotEmpty(allErrors)) {
      throw new KongIntegrationException(String.format("Failed to %s routes", operation), allErrors);
    }
  }

  private List<Parameter> updateRoutesForModule(ModuleDescriptor moduleDescriptor, String tenant) {
    var moduleId = moduleDescriptor.getId();
    var serviceId = getExistingServiceId(moduleId);
    var existingRouteNames = getKongRoutes(tenant, moduleId).stream()
      .map(Route::getName)
      .collect(toCollection(LinkedHashSet::new));

    var routes = prepareRoutes(moduleDescriptor, moduleId, tenant);
    var newRoutesCreationErrors = toStream(routes)
      .filter(not(pair1 -> existingRouteNames.contains(pair1.getLeft().getName())))
      .map(pair -> createKongRoute(serviceId, pair.getLeft(), pair.getRight()))
      .flatMap(Optional::stream)
      .toList();

    var routeNames = routes.stream()
      .map(pair -> pair.getLeft().getName())
      .collect(toSet());

    var deprecatedRoutesDeletionErrors = toStream(existingRouteNames)
      .filter(not(routeNames::contains))
      .map(routeName -> deleteRoute(serviceId, routeName))
      .flatMap(Optional::stream)
      .toList();

    var resultErrorParameters = new ArrayList<>(newRoutesCreationErrors);
    resultErrorParameters.addAll(deprecatedRoutesDeletionErrors);

    return resultErrorParameters;
  }

  private List<Parameter> addRoutesForModule(ModuleDescriptor moduleDescriptor, String tenant) {
    var moduleId = moduleDescriptor.getId();
    var serviceId = getExistingServiceId(moduleId);
    return prepareRoutes(moduleDescriptor, moduleId, tenant).stream()
      .map(kongRoutePair -> createKongRoute(serviceId, kongRoutePair.getLeft(), kongRoutePair.getRight()))
      .flatMap(Optional::stream)
      .toList();
  }

  private List<Pair<Route, RoutingEntry>> prepareRoutes(ModuleDescriptor desc, String moduleId, String tenant) {
    return toStream(desc.getProvides())
      .map(interfaceDescriptor -> prepareRoutes(interfaceDescriptor, moduleId, tenant))
      .flatMap(Collection::stream)
      .toList();
  }

  private List<Pair<Route, RoutingEntry>> prepareRoutes(InterfaceDescriptor desc, String moduleId, String tenant) {
    var interfaceId = desc.getId() + "-" + desc.getVersion();
    if (desc.isSystem()) {
      log.debug("System interface is ignored: tenant={}, moduleId={}, interfaceId={}]", tenant, moduleId, interfaceId);
      return emptyList();
    }

    var interfaceType = desc.getInterfaceType();
    var isMultiple = StringUtils.equals(interfaceType, MULTIPLE_INTERFACE_TYPE);
    return toStream(desc.getHandlers())
      .map(routingEntry -> getRoute(tenant, moduleId, interfaceId, routingEntry, isMultiple)
        .map(route -> Pair.of(route, routingEntry)))
      .flatMap(Optional::stream)
      .toList();
  }

  private String getExistingServiceId(String moduleId) {
    try {
      return kongAdminClient.getService(moduleId).getId();
    } catch (Exception exception) {
      var parameter = new Parameter().key("moduleId").value("Service is not found: test-module-0.0.1");
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

  private List<Route> getKongRoutes(String tenantName, String moduleId) {
    var routes = new ArrayList<Route>();
    var errorParameters = new ArrayList<Parameter>();
    String offset = null;
    do {
      try {
        var routePage = kongAdminClient.getRoutesByTag(getTagsToSearch(tenantName, moduleId), offset);
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

  private List<Parameter> removeKongRoutes(String tenant, String moduleId) {
    var serviceId = getExistingServiceId(moduleId);
    return getKongRoutes(tenant, moduleId).stream()
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
  private static Optional<Route> getRoute(
    String tenant, String moduleId, String interfaceId, RoutingEntry re, boolean isMultiple) {

    var staticPath = re.getStaticPath();
    var httpMethods = getMethods(re);
    if (StringUtils.isEmpty(staticPath) || CollectionUtils.isEmpty(httpMethods)) {
      log.debug("Route cannot be created: moduleId={}, tenant={}, interfaceId={}, routingEntry={}",
        () -> moduleId, () -> tenant, () -> interfaceId, () -> asString(re));
      return Optional.empty();
    }

    var kongPathPair = updatePathPatternForKongGateway(staticPath);
    var path = kongPathPair.getLeft();

    var routeName = Stream.of(path, String.join(",", httpMethods), tenant, moduleId, interfaceId)
      .filter(StringUtils::isNotBlank)
      .collect(joining("|"));

    var pathExpression = path.endsWith("$") ? httpPath().regexMatching(path) : httpPath().equalsTo(path);
    var methodsExpression = combineUsingOr(mapItems(httpMethods, method -> httpMethod().equalsTo(method)));
    var headersExpression = getHeadersExpression(tenant, moduleId, isMultiple);
    return Optional.of(
      new Route()
        .priority(kongPathPair.getRight())
        .name(sha1Hex(routeName))
        .expression(combineUsingAnd(getNonNullValues(pathExpression, methodsExpression, headersExpression)))
        .tags(getNonNullValues(tenant, moduleId, interfaceId))
        .stripPath(false)
    );
  }

  private static RouteExpression getHeadersExpression(String tenantId, String moduleId, boolean isMultiple) {
    if (tenantId == null) {
      return isMultiple ? httpHeader(MODULE_ID).equalsTo(moduleId) : null;
    }

    return isMultiple
      ? combineUsingAnd(httpHeader(TENANT).equalsTo(tenantId), httpHeader(MODULE_ID).equalsTo(moduleId))
      : httpHeader(TENANT).equalsTo(tenantId);
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
  private static <T> List<T> getNonNullValues(T ... nullableValues) {
    return Stream.of(nullableValues).filter(Objects::nonNull).toList();
  }
}
