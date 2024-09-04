package org.folio.security.integration.keycloak.service;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.folio.security.configuration.SecurityConfiguration.ROUTER_PREFIX_PROPERTY;
import static org.folio.test.TestConstants.TENANT_ID;
import static org.keycloak.OAuth2Constants.UMA_GRANT_TYPE;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import feign.FeignException.Forbidden;
import feign.FeignException.Unauthorized;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.security.domain.model.AuthUserPrincipal;
import org.folio.security.exception.ForbiddenException;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakClientProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.integration.keycloak.model.TokenResponse;
import org.folio.security.service.RoutingEntryMatcher;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.AccessToken;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.util.UrlPathHelper;

@UnitTest
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class KeycloakAuthorizationServiceTest {

  private static final String TOKEN = "Jwt token stub";
  private static final UUID AUTH_USER_ID = UUID.randomUUID();
  private static final UUID FOLIO_USER_ID = UUID.randomUUID();

  private static final String PATH = "/test-path";
  private static final String HTTP_METHOD = "GET";

  @InjectMocks private KeycloakAuthorizationService keycloakAuthorizationService;
  @Mock private Environment environment;
  @Mock private UrlPathHelper urlPathHelper;
  @Mock private KeycloakProperties properties;
  @Mock private KeycloakAuthClient keycloakClient;
  @Mock private RoutingEntryMatcher routingEntryMatcher;
  @Mock private KeycloakTokenValidator keycloakTokenValidator;

  @Mock private AccessToken accessToken;
  @Mock private TokenResponse tokenResponse;
  @Mock private HttpServletRequest httpServletRequest;

  @BeforeEach
  void setUp() {
    keycloakAuthorizationService.setEnvironment(environment);
    keycloakAuthorizationService.setUrlPathHelper(urlPathHelper);
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(environment, urlPathHelper, properties, keycloakClient,
      routingEntryMatcher, keycloakTokenValidator, accessToken);
  }

  @Test
  void authorize_positive_emptyTenantPermissions() {
    var routingEntry = routingEntry().permissionsRequired(emptyList());

    when(urlPathHelper.getPathWithinApplication(httpServletRequest)).thenReturn(PATH);
    when(httpServletRequest.getMethod()).thenReturn(HTTP_METHOD);
    when(httpServletRequest.getHeader(TENANT)).thenReturn(TENANT_ID);
    when(routingEntryMatcher.lookup(HTTP_METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(keycloakTokenValidator.validateAndDecodeToken(TOKEN)).thenReturn(accessToken);
    when(accessToken.getSubject()).thenReturn(AUTH_USER_ID.toString());
    when(accessToken.getIssuer()).thenReturn("https://keycloak/realms/" + TENANT_ID);
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("");
    when(accessToken.getOtherClaims()).thenReturn(Map.of("user_id", FOLIO_USER_ID.toString()));

    var result = keycloakAuthorizationService.authorize(httpServletRequest, TOKEN);

    assertThat(result).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
    assertThat(result.getPrincipal()).isEqualTo(authUserPrincipal());
  }

  @Test
  void authorize_positive_notMatchingTenants(CapturedOutput output) {
    var routingEntry = routingEntry().permissionsRequired(emptyList());

    when(urlPathHelper.getPathWithinApplication(httpServletRequest)).thenReturn(PATH);
    when(httpServletRequest.getMethod()).thenReturn(HTTP_METHOD);
    when(httpServletRequest.getHeader(TENANT)).thenReturn("other_tenant");
    when(routingEntryMatcher.lookup(HTTP_METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(keycloakTokenValidator.validateAndDecodeToken(TOKEN)).thenReturn(accessToken);
    when(accessToken.getSubject()).thenReturn(AUTH_USER_ID.toString());
    when(accessToken.getIssuer()).thenReturn("https://keycloak/realms/" + TENANT_ID);
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("");
    when(accessToken.getOtherClaims()).thenReturn(Map.of("user_id", FOLIO_USER_ID.toString()));

    var result = keycloakAuthorizationService.authorize(httpServletRequest, TOKEN);

    assertThat(result).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
    assertThat(result.getPrincipal()).isEqualTo(authUserPrincipal());
    assertThat(output).contains("Resolved tenant is not the same as x-okapi-tenant: "
      + "resolvedTenant = test, x-okapi-tenant = other_tenant");
  }

  @Test
  void authorize_positive_emptyTenantPermissionsTokenIsNotValid() {
    var routingEntry = routingEntry().permissionsRequired(emptyList());
    var exception = new NotAuthorizedException("Token expired");

    when(keycloakTokenValidator.validateAndDecodeToken(TOKEN)).thenThrow(exception);
    when(urlPathHelper.getPathWithinApplication(httpServletRequest)).thenReturn(PATH);
    when(httpServletRequest.getMethod()).thenReturn(HTTP_METHOD);
    when(routingEntryMatcher.lookup(HTTP_METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("");

    assertThatThrownBy(() -> keycloakAuthorizationService.authorize(httpServletRequest, TOKEN))
      .isInstanceOf(NotAuthorizedException.class)
      .hasMessage("Token expired");
  }

  @Test
  void authorize_positive_tenantPermissionsNotDefined(CapturedOutput capturedOutput) {
    var routingEntry = routingEntry();

    when(urlPathHelper.getPathWithinApplication(httpServletRequest)).thenReturn(PATH);
    when(httpServletRequest.getMethod()).thenReturn(HTTP_METHOD);
    when(properties.getClient()).thenReturn(keycloakClientProperties());
    when(routingEntryMatcher.lookup(HTTP_METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(keycloakTokenValidator.validateAndDecodeToken(TOKEN)).thenReturn(accessToken);
    when(accessToken.getSubject()).thenReturn(AUTH_USER_ID.toString());
    when(accessToken.getIssuer()).thenReturn("https://keycloak/realms/" + TENANT_ID);
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("");
    when(accessToken.getOtherClaims()).thenReturn(Map.of("user_id", FOLIO_USER_ID.toString()));
    when(keycloakClient.evaluatePermissions(authRequestParameters(), "Bearer " + TOKEN)).thenReturn(tokenResponse);

    var result = keycloakAuthorizationService.authorize(httpServletRequest, TOKEN);

    assertThat(result).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
    assertThat(result.getPrincipal()).isEqualTo(authUserPrincipal());
    assertThat(capturedOutput).contains("Evaluating user permissions to "
      + "RoutingEntry(methods=[GET], pathPattern=null, path=/test-path)");
  }

  @Test
  void authorize_positive_notAuthorized() {
    var routingEntry = routingEntry();

    when(properties.getClient()).thenReturn(keycloakClientProperties());
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("");
    when(urlPathHelper.getPathWithinApplication(httpServletRequest)).thenReturn(PATH);
    when(httpServletRequest.getMethod()).thenReturn(HTTP_METHOD);
    when(routingEntryMatcher.lookup(HTTP_METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(keycloakTokenValidator.validateAndDecodeToken(TOKEN)).thenReturn(accessToken);
    when(keycloakClient.evaluatePermissions(authRequestParameters(), "Bearer " + TOKEN)).thenThrow(Unauthorized.class);

    assertThatThrownBy(() -> keycloakAuthorizationService.authorize(httpServletRequest, TOKEN))
      .isInstanceOf(NotAuthorizedException.class)
      .hasMessage("Not authorized");
  }

  @Test
  void authorize_positive_forbiddenException() {
    var routingEntry = routingEntry();

    when(properties.getClient()).thenReturn(keycloakClientProperties());
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("");

    when(urlPathHelper.getPathWithinApplication(httpServletRequest)).thenReturn(PATH);
    when(httpServletRequest.getMethod()).thenReturn(HTTP_METHOD);
    when(routingEntryMatcher.lookup(HTTP_METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(keycloakTokenValidator.validateAndDecodeToken(TOKEN)).thenReturn(accessToken);
    when(keycloakClient.evaluatePermissions(authRequestParameters(), "Bearer " + TOKEN)).thenThrow(Forbidden.class);

    assertThatThrownBy(() -> keycloakAuthorizationService.authorize(httpServletRequest, TOKEN))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Access forbidden");
  }

  private static KeycloakClientProperties keycloakClientProperties() {
    var keycloakClientProperties = new KeycloakClientProperties();
    keycloakClientProperties.setClientId("be-test-admin-client");
    return keycloakClientProperties;
  }

  private static Map<String, ?> authRequestParameters() {
    return Map.of(
      "grant_type", UMA_GRANT_TYPE,
      "audience", "be-test-admin-client",
      "permission", PATH + "#" + HTTP_METHOD);
  }

  private static AuthUserPrincipal authUserPrincipal() {
    return new AuthUserPrincipal()
      .userId(FOLIO_USER_ID)
      .authUserId(AUTH_USER_ID.toString())
      .tenant(TENANT_ID);
  }

  private static RoutingEntry routingEntry() {
    return new RoutingEntry()
      .path(PATH)
      .methods(List.of(HTTP_METHOD))
      .permissionsRequired(List.of("test.permission"));
  }
}
