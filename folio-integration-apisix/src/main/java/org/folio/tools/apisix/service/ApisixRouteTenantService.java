package org.folio.tools.apisix.service;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.tools.apisix.model.ApisixRoute;
import org.jspecify.annotations.Nullable;

/**
 * Structural manipulation of the tenant condition inside an APISIX route's {@code vars} expression.
 */
@Log4j2
public class ApisixRouteTenantService {

  static final String TENANT_VAR = "http_x_okapi_tenant";
  static final String IN_OPERATOR = "in";
  static final String REGEX_OPERATOR = "~~";
  static final String WILDCARD_REGEX = ".*";

  /**
   * Adds a tenant to the route's tenant {@code vars} condition. No changes when the tenant is already present.
   *
   * @param route - the route to update
   * @param tenantId - the tenant identifier to add
   * @return {@code true} if the route's vars were modified
   */
  public boolean addTenant(ApisixRoute route, String tenantId) {
    if (isInvalidInput(route, tenantId)) {
      return false;
    }

    var vars = route.getVars();
    var tenantVar = findTenantVar(vars);
    if (tenantVar == null) {
      vars.add(tenantVar(List.of(tenantId)));
      return true;
    }

    if (isWildcard(tenantVar)) {
      replace(vars, tenantVar, tenantVar(List.of(tenantId)));
      return true;
    }

    return addTenantToVar(vars, tenantVar, tenantId, route.getId());
  }

  /**
   * Removes a tenant from the route's tenant {@code vars} condition. Restores the wildcard condition when the last
   * tenant is removed. No changes when the tenant is absent or the condition is the wildcard.
   *
   * @param route - the route to update
   * @param tenantId - the tenant identifier to remove
   * @return {@code true} if the route's vars were modified
   */
  public boolean removeTenant(ApisixRoute route, String tenantId) {
    if (isInvalidInput(route, tenantId)) {
      return false;
    }

    var vars = route.getVars();
    var tenantVar = findTenantVar(vars);
    if (tenantVar == null || !isTenantList(tenantVar)) {
      log.debug("Route [{}] has no tenant list condition, nothing to remove", route.getId());
      return false;
    }

    var tenants = getTenants(tenantVar);
    if (!tenants.contains(tenantId)) {
      log.debug("Tenant [{}] does not exist in route [{}] vars", tenantId, route.getId());
      return false;
    }

    var remaining = new ArrayList<>(tenants);
    remaining.remove(tenantId);
    replace(vars, tenantVar, remaining.isEmpty() ? wildcardTenantVar() : tenantVar(remaining));
    return true;
  }

  private static boolean addTenantToVar(List<List<Object>> vars, List<Object> tenantVar, String tenantId,
    String routeId) {
    if (!isTenantList(tenantVar)) {
      log.warn("Route [{}] tenant condition is not modifiable: {}", routeId, tenantVar);
      return false;
    }

    var tenants = getTenants(tenantVar);
    if (tenants.contains(tenantId)) {
      log.debug("Tenant [{}] already exists in route [{}] vars", tenantId, routeId);
      return false;
    }

    var updated = new ArrayList<>(tenants);
    updated.add(tenantId);
    replace(vars, tenantVar, tenantVar(updated));
    return true;
  }

  private static boolean isInvalidInput(ApisixRoute route, String tenantId) {
    if (route == null || isBlank(tenantId)) {
      log.warn("Cannot process tenant: route or tenantId is null/blank");
      return true;
    }

    if (isEmpty(route.getVars())) {
      log.warn("Cannot process tenant for route [{}]: vars is null/empty", route.getId());
      return true;
    }

    return false;
  }

  private static @Nullable List<Object> findTenantVar(List<List<Object>> vars) {
    return vars.stream()
      .filter(var -> !var.isEmpty() && TENANT_VAR.equals(var.get(0)))
      .findFirst()
      .orElse(null);
  }

  private static boolean isWildcard(List<Object> tenantVar) {
    return tenantVar.size() == 3
      && REGEX_OPERATOR.equals(tenantVar.get(1))
      && WILDCARD_REGEX.equals(tenantVar.get(2));
  }

  private static boolean isTenantList(List<Object> tenantVar) {
    return tenantVar.size() == 3 && IN_OPERATOR.equals(tenantVar.get(1)) && tenantVar.get(2) instanceof Collection<?>;
  }

  private static List<String> getTenants(List<Object> tenantVar) {
    return ((Collection<?>) tenantVar.get(2)).stream().map(Object::toString).toList();
  }

  private static List<Object> tenantVar(List<String> tenants) {
    return List.of(TENANT_VAR, IN_OPERATOR, List.copyOf(tenants));
  }

  private static List<Object> wildcardTenantVar() {
    return List.of(TENANT_VAR, REGEX_OPERATOR, WILDCARD_REGEX);
  }

  private static void replace(List<List<Object>> vars, List<Object> oldVar, List<Object> newVar) {
    vars.set(vars.indexOf(oldVar), newVar);
  }
}
