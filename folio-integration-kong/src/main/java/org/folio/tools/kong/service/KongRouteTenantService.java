package org.folio.tools.kong.service;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tools.kong.model.Route;

@Log4j2
@RequiredArgsConstructor
public class KongRouteTenantService {

  private static final String TENANT_HEADER = "http.headers.x_okapi_tenant";
  private static final String TENANT_HEADER_REGEX = "http\\.headers\\.x_okapi_tenant\\s*~\\s*r#\".*\"#";
  private static final String WILDCARD_TENANT_EXPRESSION = "http.headers.x_okapi_tenant ~ r#\".*\"#";

  // Matches a tenant clause consisting of one or multiple header equality checks joined by ||, wrapped in parentheses.
  // Examples that will match:
  //   (http.headers.x_okapi_tenant == "t1")
  //   (http.headers.x_okapi_tenant == "t1" || http.headers.x_okapi_tenant == "t2")
  private static final Pattern TENANT_CLAUSE_PATTERN = Pattern.compile(
    "\\(" // opening parenthesis (required)
      + "\\s*" + Pattern.quote(TENANT_HEADER) + "\\s*==\\s*\"[^\"]+\"" // first comparison
      + "(?:\\s*\\|\\|\\s*" + Pattern.quote(TENANT_HEADER) + "\\s*==\\s*\"[^\"]+\")*" // additional comparisons
      + "\\s*" + "\\)" // closing parenthesis (required)
  );

  // Pattern to match a single tenant equality check
  private static final Pattern SINGLE_TENANT_PATTERN = Pattern.compile(
    Pattern.quote(TENANT_HEADER) + "\\s*==\\s*\"([^\"]+)\""
  );

  /**
   * Adds a tenant to the route expression. If the tenant already exists in the expression, no changes are made.
   *
   * @param route    - the route to update
   * @param tenantId - the tenant identifier to add
   * @return the updated route with the tenant added to the expression
   */
  public Route addTenant(Route route, String tenantId) {
    if (!isValidInput(route, tenantId)) {
      return route;
    }

    var expression = route.getExpression();
    if (!isValidExpression(expression, route.getId())) {
      return route;
    }

    log.debug("Adding tenant [{}] to route [id: {}, expression: {}]", tenantId, route.getId(), expression);

    if (expression.matches(".*" + TENANT_HEADER_REGEX + ".*")) {
      return replaceWildcardPattern(route, expression, tenantId);
    }

    return addTenantToExpression(route, expression, tenantId);
  }

  /**
   * Removes a tenant from the route expression. If the tenant doesn't exist in the expression, no changes are made.
   *
   * @param route    - the route to update
   * @param tenantId - the tenant identifier to remove
   * @return the updated route with the tenant removed from the expression
   */
  public Route removeTenant(Route route, String tenantId) {
    if (!isValidInput(route, tenantId)) {
      return route;
    }

    var expression = route.getExpression();
    if (!isValidExpression(expression, route.getId())) {
      return route;
    }

    log.debug("Removing tenant [{}] from route [id: {}, expression: {}]", tenantId, route.getId(), expression);

    if (expression.matches(".*" + TENANT_HEADER_REGEX + ".*")) {
      log.debug("Route expression contains wildcard tenant pattern, cannot remove specific tenant");
      return route;
    }

    return removeTenantFromExpression(route, expression, tenantId);
  }

  private static boolean isValidInput(Route route, String tenantId) {
    if (route == null || isBlank(tenantId)) {
      log.warn("Cannot process tenant: route or tenantId is null/blank");
      return false;
    }
    return true;
  }

  private static boolean isValidExpression(String expression, String routeId) {
    if (isBlank(expression)) {
      log.warn("Cannot process tenant for route [id: {}]: expression is null/blank", routeId);
      return false;
    }
    return true;
  }

  private static Route replaceWildcardPattern(Route route, String expression, String tenantId) {
    log.debug("Route expression contains wildcard tenant pattern, replacing with specific tenant");
    var updatedExpression = replaceWildcardWithTenant(expression, tenantId);
    route.setExpression(updatedExpression);
    log.debug("Updated route expression: {}", updatedExpression);
    return route;
  }

  private static Route addTenantToExpression(Route route, String expression, String tenantId) {
    var matcher = TENANT_CLAUSE_PATTERN.matcher(expression);
    if (matcher.find()) {
      var tenantClause = matcher.group();

      if (containsTenant(tenantClause, tenantId)) {
        log.debug("Tenant [{}] already exists in route expression", tenantId);
        return route;
      }

      var updatedClause = addTenantToClause(tenantClause, tenantId);
      var updatedExpression = expression.replace(tenantClause, updatedClause);
      route.setExpression(updatedExpression);
      log.debug("Updated route expression: {}", updatedExpression);
      return route;
    }

    var tenantClause = createTenantClause(tenantId);
    var updatedExpression = expression + " && " + tenantClause;
    route.setExpression(updatedExpression);
    log.debug("Updated route expression: {}", updatedExpression);
    return route;
  }

  private static Route removeTenantFromExpression(Route route, String expression, String tenantId) {
    var matcher = TENANT_CLAUSE_PATTERN.matcher(expression);
    if (!matcher.find()) {
      log.debug("Route expression does not contain tenant clause, nothing to remove");
      return route;
    }

    var tenantClause = matcher.group();

    if (!containsTenant(tenantClause, tenantId)) {
      log.debug("Tenant [{}] does not exist in route expression", tenantId);
      return route;
    }

    var updatedClause = removeTenantFromClause(tenantClause, tenantId);

    if (isBlank(updatedClause)) {
      replaceTenantClauseWithPlaceholder(route, expression, tenantClause);
    } else {
      var updatedExpression = expression.replace(tenantClause, updatedClause);
      route.setExpression(updatedExpression);
      log.debug("Updated route expression: {}", updatedExpression);
    }

    return route;
  }

  private static void replaceTenantClauseWithPlaceholder(Route route, String expression, String tenantClause) {
    var wildcardClause = "(" + WILDCARD_TENANT_EXPRESSION + ")";
    var updatedExpression = expression.replace(tenantClause, wildcardClause);
    route.setExpression(updatedExpression);
    log.debug("Replaced tenant clause with wildcard expression, updated route expression: {}", updatedExpression);
  }

  /**
   * Replaces wildcard tenant pattern with specific tenant clause.
   */
  private static String replaceWildcardWithTenant(String expression, String tenantId) {
    var pattern = Pattern.compile("\\s*\\(?\\s*" + TENANT_HEADER_REGEX + "\\s*\\)?");
    var matcher = pattern.matcher(expression);

    if (matcher.find()) {
      var matched = matcher.group();
      var tenantClause = createTenantClause(tenantId);
      var replacement = buildReplacement(matched, tenantClause);
      return matcher.replaceFirst(replacement);
    }

    return expression;
  }

  private static String buildReplacement(String matched, String tenantClause) {
    var leadingSpace = extractLeadingWhitespace(matched);
    var trimmed = matched.trim();

    if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
      return leadingSpace + tenantClause;
    }

    if (trimmed.endsWith(")")) {
      return leadingSpace + tenantClause + ")";
    }

    return leadingSpace + tenantClause;
  }

  private static String extractLeadingWhitespace(String str) {
    var leadingSpace = new StringBuilder();
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == ' ' || c == '\t') {
        leadingSpace.append(c);
      } else {
        break;
      }
    }
    return leadingSpace.toString();
  }

  /**
   * Checks if a tenant exists in the tenant clause.
   */
  private static boolean containsTenant(String tenantClause, String tenantId) {
    var matcher = SINGLE_TENANT_PATTERN.matcher(tenantClause);
    while (matcher.find()) {
      if (tenantId.equals(matcher.group(1))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Creates a new tenant clause with the given tenant ID.
   */
  private static String createTenantClause(String tenantId) {
    return "(" + TENANT_HEADER + " == \"" + tenantId + "\")";
  }

  /**
   * Adds a tenant to an existing tenant clause.
   */
  private static String addTenantToClause(String tenantClause, String tenantId) {
    // Remove the closing parenthesis
    var withoutClosing = tenantClause.substring(0, tenantClause.lastIndexOf(')'));
    return withoutClosing + " || " + TENANT_HEADER + " == \"" + tenantId + "\")";
  }

  /**
   * Removes a tenant from a tenant clause. Returns null if the clause becomes empty.
   */
  private static String removeTenantFromClause(String tenantClause, String tenantId) {
    var tenants = extractTenants(tenantClause);
    tenants.remove(tenantId);

    if (tenants.isEmpty()) {
      return null;
    }

    if (tenants.size() == 1) {
      return createTenantClause(tenants.get(0));
    }

    var clause = new StringBuilder("(");
    for (int i = 0; i < tenants.size(); i++) {
      if (i > 0) {
        clause.append(" || ");
      }
      clause.append(TENANT_HEADER).append(" == \"").append(tenants.get(i)).append("\"");
    }
    clause.append(")");

    return clause.toString();
  }

  /**
   * Extracts all tenant IDs from a tenant clause.
   */
  private static List<String> extractTenants(String tenantClause) {
    var tenants = new ArrayList<String>();
    var matcher = SINGLE_TENANT_PATTERN.matcher(tenantClause);
    while (matcher.find()) {
      tenants.add(matcher.group(1));
    }
    return tenants;
  }
}
