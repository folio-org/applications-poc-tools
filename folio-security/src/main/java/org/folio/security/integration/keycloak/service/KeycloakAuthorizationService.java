package org.folio.security.integration.keycloak.service;

import static java.util.Map.entry;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.folio.security.integration.keycloak.service.KeycloakTokenValidator.resolveTenant;
import static org.keycloak.OAuth2Constants.UMA_GRANT_TYPE;

import feign.FeignException;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.jwt.openid.JsonWebTokenParser;
import org.folio.security.domain.model.AuthUserPrincipal;
import org.folio.security.exception.ForbiddenException;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.exception.RoutingEntryMatchingException;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.service.AbstractAuthorizationService;
import org.folio.security.service.RoutingEntryMatcher;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

@Log4j2
@RequiredArgsConstructor
public class KeycloakAuthorizationService extends AbstractAuthorizationService {

  private final KeycloakProperties properties;
  private final KeycloakAuthClient keycloakClient;
  private final RoutingEntryMatcher routingEntryMatcher;
  private final JsonWebTokenParser jsonWebTokenParser;

  @Override
  public Authentication authorize(HttpServletRequest request, String token) {
    var path = getRequestPath(request);
    var method = request.getMethod();
    var routingEntry = routingEntryMatcher.lookup(method, path)
      .orElseThrow(() -> new RoutingEntryMatchingException("Unable to resolve routing entry for path: " + path));

    JsonWebToken accessToken;
    try {
      accessToken = jsonWebTokenParser.parse(token);
    } catch (ParseException e) {
      throw new NotAuthorizedException("Not authorized");
    }
    return isEmpty(routingEntry.getPermissionsRequired())
      ? checkTenantMatching(accessToken, request)
      : evaluatePermissions(routingEntry, method, accessToken, token);
  }

  private Authentication checkTenantMatching(JsonWebToken accessToken, HttpServletRequest request) {
    var headerTenant = request.getHeader(TENANT);
    var resolvedTenant = resolveTenant(accessToken.getIssuer());
    if (!Objects.equals(resolvedTenant, headerTenant)) {
      log.debug("Resolved tenant is not the same as x-okapi-tenant: resolvedTenant = {}, x-okapi-tenant = {}",
        resolvedTenant, headerTenant);
    }

    return createAuthentication(accessToken);
  }

  private Authentication evaluatePermissions(RoutingEntry re, String method, JsonWebToken jwt, String jwtStr) {
    log.info("Evaluating user permissions to {}", re);
    var body = prepareRequestBody(re, method);
    try {
      keycloakClient.evaluatePermissions(body, "Bearer " + jwtStr);
      return createAuthentication(jwt);
    } catch (FeignException.Forbidden e) {
      throw new ForbiddenException("Access forbidden", e);
    } catch (FeignException.Unauthorized e) {
      throw new NotAuthorizedException("Not authorized", e);
    }
  }

  private Map<String, ?> prepareRequestBody(RoutingEntry routingEntry, String scope) {
    var resource = StringUtils.getIfEmpty(routingEntry.getPath(), routingEntry::getPathPattern);
    return Map.ofEntries(
      entry("grant_type", UMA_GRANT_TYPE),
      entry("audience", properties.getClient().getClientId()),
      entry("permission", resource + "#" + scope));
  }

  private static PreAuthenticatedAuthenticationToken createAuthentication(JsonWebToken accessToken) {
    var authUserPrincipal = new AuthUserPrincipal()
      .userId(getFolioUserId(accessToken))
      .authUserId(accessToken.getSubject())
      .tenant(resolveTenant(accessToken.getIssuer()));

    return new PreAuthenticatedAuthenticationToken(authUserPrincipal, null, Collections.emptyList());
  }

  private static UUID getFolioUserId(JsonWebToken accessToken) {
    return ofNullable(accessToken)
      .map(token -> token.getClaim("user_id"))
      .filter(String.class::isInstance)
      .map(String.class::cast)
      .map(UUID::fromString)
      .orElse(null);
  }
}
