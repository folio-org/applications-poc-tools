package org.folio.tools.apisix.service;

import static java.util.Collections.emptyList;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.folio.common.gateway.utils.GatewayRouteUtils.buildRouteName;
import static org.folio.common.gateway.utils.GatewayRouteUtils.getMethods;
import static org.folio.common.gateway.utils.GatewayRouteUtils.preparePath;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.tools.apisix.service.ApisixRouteTenantService.REGEX_OPERATOR;
import static org.folio.tools.apisix.service.ApisixRouteTenantService.TENANT_VAR;
import static org.folio.tools.apisix.service.ApisixRouteTenantService.WILDCARD_REGEX;
import static org.springframework.util.ObjectUtils.nullSafeEquals;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.common.domain.model.InterfaceDescriptor;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.tools.apisix.model.ApisixRoute;

/**
 * Translates FOLIO module descriptors into APISIX routes.
 *
 * <p>Path patterns with variables or wildcards become a prefix-wildcard {@code uri} narrowed by an anchored
 * regex condition in {@code vars}; static paths become exact {@code uri} matches with a higher priority.</p>
 */
@Log4j2
public class ApisixRouteFactory {

  public static final String MULTIPLE_INTERFACE_TYPE = "multiple";
  public static final String MODULE_LABEL = "module";
  public static final String INTERFACE_LABEL = "interface";

  private static final String MODULE_ID_VAR = "http_x_okapi_module_id";

  /**
   * Creates APISIX routes for all non-system interfaces of the given module descriptor.
   *
   * @param descriptor - module descriptor to process
   * @param serviceId - id of the APISIX service the routes belong to
   * @return list of derived {@link ApisixRoute} objects
   */
  public List<ApisixRoute> createRoutes(ModuleDescriptor descriptor, String serviceId) {
    return toStream(descriptor.getProvides())
      .map(interfaceDescriptor ->
        createRoutes(interfaceDescriptor, descriptor.getId(), serviceId, descriptor.isMgrComponent()))
      .flatMap(Collection::stream)
      .toList();
  }

  private static List<ApisixRoute> createRoutes(InterfaceDescriptor desc, String moduleId, String serviceId,
    boolean isMgrComponent) {
    var interfaceId = desc.getId() + "-" + desc.getVersion();
    if (desc.isSystem()) {
      log.debug("System interface is ignored: moduleId={}, interfaceId={}", moduleId, interfaceId);
      return emptyList();
    }

    var isMultiple = nullSafeEquals(desc.getInterfaceType(), MULTIPLE_INTERFACE_TYPE);
    return toStream(desc.getHandlers())
      .map(routingEntry -> createRoute(moduleId, interfaceId, serviceId, routingEntry, isMgrComponent, isMultiple))
      .flatMap(Optional::stream)
      .toList();
  }

  private static Optional<ApisixRoute> createRoute(String moduleId, String interfaceId, String serviceId,
    RoutingEntry re, boolean isMgrComponent, boolean isMultiple) {
    var staticPath = re.getStaticPath();
    var httpMethods = getMethods(re);
    if (StringUtils.isEmpty(staticPath) || CollectionUtils.isEmpty(httpMethods)) {
      log.debug("Route cannot be created: moduleId={}, interfaceId={}", moduleId, interfaceId);
      return Optional.empty();
    }

    var pathPair = preparePath(staticPath);
    var path = pathPair.getLeft();
    var routeId = sha1Hex(buildRouteName(moduleId, interfaceId, path, httpMethods));
    var vars = buildVars(path, moduleId, isMgrComponent, isMultiple);
    return Optional.of(new ApisixRoute()
      .id(routeId).name(routeId)
      .uri(buildUri(staticPath, pathPair.getRight()))
      .priority(pathPair.getRight())
      .status(1)
      .methods(httpMethods)
      .serviceId(serviceId)
      .labels(Map.of(MODULE_LABEL, moduleId, INTERFACE_LABEL, interfaceId))
      .vars(vars.isEmpty() ? null : vars));
  }

  private static List<List<Object>> buildVars(String path, String moduleId, boolean isMgrComponent,
    boolean isMultiple) {
    var uriVar = path.endsWith("$") ? List.<Object>of("uri", REGEX_OPERATOR, path) : null;
    var moduleIdVar = isMultiple ? List.<Object>of(MODULE_ID_VAR, "==", moduleId) : null;
    var tenantVar = isMgrComponent ? null : List.<Object>of(TENANT_VAR, REGEX_OPERATOR, WILDCARD_REGEX);
    return Stream.of(uriVar, moduleIdVar, tenantVar).filter(Objects::nonNull).toList();
  }

  /**
   * APISIX radixtree matches {@code uri} exactly or by a trailing prefix wildcard; patterns with path variables
   * are matched by the longest static prefix here and narrowed by the anchored regex condition in {@code vars}.
   */
  private static String buildUri(String staticPath, int priority) {
    if (priority == 1) {
      return staticPath;
    }

    var tokenIndex = StringUtils.indexOfAny(staticPath, '{', '}', '*');
    var prefix = staticPath.substring(0, tokenIndex);
    return prefix.startsWith("/") ? prefix + "*" : "/*";
  }
}
