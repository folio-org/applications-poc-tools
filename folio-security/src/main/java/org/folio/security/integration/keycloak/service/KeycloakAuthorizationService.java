package org.folio.security.integration.keycloak.service;

import static java.util.Map.entry;
import static org.folio.security.integration.keycloak.service.KeycloakTokenValidator.resolveTenant;
import static org.keycloak.OAuth2Constants.UMA_GRANT_TYPE;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.common.utils.OkapiHeaders;
import org.folio.security.domain.model.descriptor.RoutingEntry;
import org.folio.security.exception.ForbiddenException;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.exception.RoutingEntryMatchingException;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.service.AuthorizationService;
import org.folio.security.service.RoutingEntryMatcher;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.util.UrlPathHelper;

@Log4j2
@RequiredArgsConstructor
public class KeycloakAuthorizationService implements AuthorizationService {

  private final UrlPathHelper urlPathHelper = new UrlPathHelper();
  private final KeycloakAuthClient keycloakClient;
  private final RoutingEntryMatcher routingEntryMatcher;
  private final KeycloakTokenValidator tokenValidator;
  private final KeycloakProperties properties;

  @Override
  public Authentication authorize(HttpServletRequest request, String token) {
    var path = urlPathHelper.getPathWithinApplication(request);
    var method = request.getMethod();
    var routingEntry = routingEntryMatcher.lookup(method, path)
      .orElseThrow(() -> new RoutingEntryMatchingException("Unable to resolve routing entry for path: " + path));

    return hasPermissionsRequiredTenant(routingEntry)
      ? evaluateTenantPermissions(request, token)
      : evaluatePermissions(routingEntry, method, token);
  }

  private static boolean hasPermissionsRequiredTenant(RoutingEntry routingEntry) {
    var permissionsRequiredTenant = routingEntry.getPermissionsRequiredTenant();
    // currently only empty tenant permissions are supported
    return permissionsRequiredTenant != null && permissionsRequiredTenant.isEmpty();
  }

  private Authentication evaluateTenantPermissions(HttpServletRequest request, String token) {
    var headerTenant = request.getHeader(OkapiHeaders.TENANT);
    if (StringUtils.isBlank(headerTenant)) {
      throw new IllegalArgumentException("X-Okapi-Tenant header is required");
    }
    var accessToken = tokenValidator.validateAndDecodeToken(token);
    var resolvedTenant = resolveTenant(accessToken.getIssuer());
    if (resolvedTenant.equals(headerTenant)) {
      return createAuthentication();
    } else {
      throw new ForbiddenException("X-Okapi-Tenant header is not the same as resolved tenant");
    }
  }

  private Authentication evaluatePermissions(RoutingEntry routingEntry, String method, String token) {
    log.info("Evaluating user permissions to {}", routingEntry);
    var body = prepareRequestBody(routingEntry, method);
    try {
      keycloakClient.evaluatePermissions(body, "Bearer " + token);
      return createAuthentication();
    } catch (FeignException.Forbidden e) {
      throw new ForbiddenException("Access forbidden");
    } catch (FeignException.Unauthorized e) {
      throw new NotAuthorizedException("Not authorized");
    }
  }

  private Map<String, ?> prepareRequestBody(RoutingEntry routingEntry, String scope) {
    var resource = StringUtils.getIfEmpty(routingEntry.getPath(), routingEntry::getPathPattern);
    return Map.ofEntries(
      entry("grant_type", UMA_GRANT_TYPE),
      entry("audience", properties.getClient().getClientId()),
      entry("permission", resource + "#" + scope));
  }

  private static PreAuthenticatedAuthenticationToken createAuthentication() {
    return new PreAuthenticatedAuthenticationToken(null, null, Collections.emptyList());
  }
}
