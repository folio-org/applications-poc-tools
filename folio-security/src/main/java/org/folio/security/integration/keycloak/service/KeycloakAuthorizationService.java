package org.folio.security.integration.keycloak.service;

import static java.util.Map.entry;
import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.folio.security.integration.keycloak.service.KeycloakTokenValidator.resolveTenant;
import static org.keycloak.OAuth2Constants.UMA_GRANT_TYPE;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.security.domain.AuthUserPrincipal;
import org.folio.security.exception.ForbiddenException;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.exception.RoutingEntryMatchingException;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.service.AbstractAuthorizationService;
import org.folio.security.service.RoutingEntryMatcher;
import org.keycloak.representations.AccessToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.util.UrlPathHelper;

@Log4j2
@RequiredArgsConstructor
public class KeycloakAuthorizationService extends AbstractAuthorizationService {

  private final UrlPathHelper urlPathHelper = new UrlPathHelper();
  private final KeycloakAuthClient keycloakClient;
  private final RoutingEntryMatcher routingEntryMatcher;
  private final KeycloakTokenValidator tokenValidator;
  private final KeycloakProperties properties;

  @Override
  public Authentication authorize(HttpServletRequest request, String token) {
    var path = updatePath(urlPathHelper.getPathWithinApplication(request));
    var method = request.getMethod();
    var routingEntry = routingEntryMatcher.lookup(method, path)
      .orElseThrow(() -> new RoutingEntryMatchingException("Unable to resolve routing entry for path: " + path));

    var accessToken = tokenValidator.validateAndDecodeToken(token);
    return hasPermissionsRequiredTenant(routingEntry)
        ? evaluateTenantPermissions(accessToken, request)
        : evaluatePermissions(routingEntry, method, accessToken, token);
  }

  private static boolean hasPermissionsRequiredTenant(RoutingEntry routingEntry) {
    var permissionsRequiredTenant = routingEntry.getPermissionsRequiredTenant();
    // currently only empty tenant permissions are supported
    return permissionsRequiredTenant != null && permissionsRequiredTenant.isEmpty();
  }

  private Authentication evaluateTenantPermissions(AccessToken accessToken, HttpServletRequest request) {
    var headerTenant = request.getHeader(TENANT);
    var resolvedTenant = resolveTenant(accessToken.getIssuer());
    if (!Objects.equals(resolvedTenant, headerTenant)) {
      log.debug("Resolved tenant is not the same as x-okapi-tenant: resolvedTenant = {}, x-okapi-tenant = {}",
          resolvedTenant, headerTenant);
    }

    return createAuthentication(accessToken);
  }

  private Authentication evaluatePermissions(RoutingEntry routingEntry, String method, AccessToken jwt, String jwtStr) {
    log.info("Evaluating user permissions to {}", routingEntry);
    var body = prepareRequestBody(routingEntry, method);
    try {
      keycloakClient.evaluatePermissions(body, "Bearer " + jwtStr);
      return createAuthentication(jwt);
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

  private static PreAuthenticatedAuthenticationToken createAuthentication(AccessToken accessToken) {
    var authUserPrincipal = new AuthUserPrincipal()
        .authUserId(accessToken.getSubject())
        .tenant(resolveTenant(accessToken.getIssuer()));
    return new PreAuthenticatedAuthenticationToken(authUserPrincipal, null, Collections.emptyList());
  }
}
