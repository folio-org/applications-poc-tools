package org.folio.security.integration.authtoken.service;

import static java.util.Collections.emptyMap;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.common.utils.OkapiHeaders.SUPERTENANT_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.security.domain.AuthUserPrincipal;
import org.folio.security.domain.OkapiAccessToken;
import org.folio.security.exception.ForbiddenException;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.exception.RoutingEntryMatchingException;
import org.folio.security.integration.authtoken.client.AuthtokenClient;
import org.folio.security.service.AbstractAuthorizationService;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.folio.security.service.RoutingEntryMatcher;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.util.UrlPathHelper;

@RequiredArgsConstructor
public class OkapiAuthorizationService extends AbstractAuthorizationService {

  private static final String COMMA = ",";
  private static final String TOKEN_SEPARATOR = "\\.";
  private static final String INVALID_SEGMENTS_JWT_ERROR_MSG = "Invalid amount of segments in JsonWebToken.";

  private final ObjectMapper objectMapper;
  private final UrlPathHelper urlPathHelper;
  private final RoutingEntryMatcher routingEntryMatcher;
  private final InternalModuleDescriptorProvider descriptorProvider;
  private final AuthtokenClient client;
  private final String okapiUrl;

  @Override
  public Authentication authorize(HttpServletRequest request, String token) {
    var path = updatePath(urlPathHelper.getPathWithinApplication(request));
    var method = request.getMethod();

    var routingEntry = routingEntryMatcher.lookup(method, path)
      .orElseThrow(() -> new RoutingEntryMatchingException("Unable to resolve routing entry for path: " + path));

    var requiredPermissions = extractPermissionsCommaSplit(routingEntry.getPermissionsRequired());
    var desiredPermissions = extractPermissionsCommaSplit(routingEntry.getPermissionsDesired());
    var modulePermissions = extractModulePermissions(routingEntry);

    try {
      client.checkAuthToken(path, requiredPermissions, desiredPermissions, modulePermissions,
        token, SUPERTENANT_ID, okapiUrl);
    } catch (FeignException.Forbidden e) {
      throw new ForbiddenException("Access forbidden");
    } catch (FeignException.Unauthorized e) {
      throw new NotAuthorizedException("Not authorized");
    }
    return createAuthentication(token);
  }

  private Map<String, List<String>> extractModulePermissions(RoutingEntry routingEntry) {
    var modPerms = routingEntry.getModulePermissions();
    if (CollectionUtils.isNotEmpty(modPerms)) {
      var modPermissionsGroupedByModuleId = new HashMap<String, List<String>>();
      var moduleId = descriptorProvider.getModuleDescriptor().getId();
      modPermissionsGroupedByModuleId.put(moduleId, modPerms);
      return modPermissionsGroupedByModuleId;
    }
    return emptyMap();
  }

  private String extractPermissionsCommaSplit(List<String> permissions) {
    if (isEmpty(permissions)) {
      return null;
    }
    return String.join(COMMA, permissions);
  }

  private PreAuthenticatedAuthenticationToken createAuthentication(String jwt) {
    return new PreAuthenticatedAuthenticationToken(getAuthUserPrincipal(jwt), null, Collections.emptyList());
  }

  private AuthUserPrincipal getAuthUserPrincipal(String jwt) {
    var parsedOkapiToken = getParsedJwtBody(jwt);
    var userId = parsedOkapiToken.getUserId();
    return new AuthUserPrincipal()
      .tenant(parsedOkapiToken.getTenant())
      .userId(userId)
      .authUserId(Objects.toString(userId, null));
  }

  private OkapiAccessToken getParsedJwtBody(String authToken) {
    var split = authToken.split(TOKEN_SEPARATOR);
    if (split.length < 2 || split.length > 3) {
      throw new NotAuthorizedException(INVALID_SEGMENTS_JWT_ERROR_MSG);
    }

    try {
      var decodedValue = new String(Base64.getDecoder().decode(split[1]));
      return objectMapper.readValue(decodedValue, OkapiAccessToken.class);
    } catch (Exception exception) {
      throw new NotAuthorizedException("Failed to read JsonWebToken body", exception);
    }
  }
}
