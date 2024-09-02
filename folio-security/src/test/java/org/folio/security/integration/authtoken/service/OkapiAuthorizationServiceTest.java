package org.folio.security.integration.authtoken.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.OkapiHeaders.SUPERTENANT_ID;
import static org.folio.security.service.AbstractAuthorizationService.ROUTER_PREFIX_PROPERTY;
import static org.folio.test.TestUtils.OBJECT_MAPPER;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.security.domain.AuthUserPrincipal;
import org.folio.security.domain.OkapiAccessToken;
import org.folio.security.exception.ForbiddenException;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.exception.RoutingEntryMatchingException;
import org.folio.security.integration.authtoken.client.AuthtokenClient;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.folio.security.service.RoutingEntryMatcher;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.util.UrlPathHelper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class OkapiAuthorizationServiceTest {

  /**
   * Sample JWT that will expire in 2030 year for test_tenant with randomly generated user id.
   */
  private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmb2xpbyIsInVzZXJfaWQiOiJlNmQyODVlOS03MmVkLTQxYT"
    + "QtOGIzYi01Y2VlNGNiYzg0MjUiLCJ0eXBlIjoiYWNjZXNzIiwiZXhwIjoxODkzNTAyODAwLCJpYXQiOjE3MjUzMDM2ODgsInRlbmFudCI6InRlc"
    + "3RfdGVuYW50In0.SdtIQTrn7_XPnyi75Ai9bBkCWa8eQ69U6VAidCCRFFQ";

  private static final UUID USER_ID = UUID.fromString("e6d285e9-72ed-41a4-8b3b-5cee4cbc8425");
  private static final String TOKEN_CONTENT = "{"
    + "\"sub\":\"folio\",\"user_id\":\"" + USER_ID + "\","
    + "\"type\":\"access\",\"exp\":1893502800,\"iat\":1725303688,"
    + "\"tenant\":\"test_tenant\"}";

  private static final String PATH = "/test";
  private static final String METHOD = "GET";
  private static final String PERMISSION_1 = "permission1";
  private static final String PERMISSION_2 = "permission2";
  private static final String MODULE_PERMISSION_1 = "module_permission1";
  private static final String MODULE_PERMISSION_2 = "module_permission2";
  private static final String MODULE_ID = "module_id";

  @Mock private Environment environment;
  @Mock private UrlPathHelper urlPathHelper;
  @Mock private RoutingEntryMatcher routingEntryMatcher;
  @Mock private InternalModuleDescriptorProvider descriptorProvider;
  @Mock private AuthtokenClient client;
  @Mock private HttpServletRequest request;
  @Spy private ObjectMapper objectMapper = OBJECT_MAPPER;

  @InjectMocks private OkapiAuthorizationService service;

  @BeforeEach
  void setup() {
    service.setEnvironment(environment);
  }

  @Test
  void authorize_positive() throws JsonProcessingException {
    var permissionsRequired = List.of(PERMISSION_1, PERMISSION_2);
    var modulePermissions = List.of(MODULE_PERMISSION_1, MODULE_PERMISSION_2);
    var routingEntry = new RoutingEntry().path(PATH).methods(List.of(METHOD)).permissionsRequired(permissionsRequired)
      .modulePermissions(modulePermissions);
    var moduleDescriptor = new ModuleDescriptor().id(MODULE_ID);

    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("/");
    when(request.getMethod()).thenReturn(METHOD);
    when(urlPathHelper.getPathWithinApplication(request)).thenReturn(PATH);
    when(routingEntryMatcher.lookup(METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(descriptorProvider.getModuleDescriptor()).thenReturn(moduleDescriptor);

    var auth = service.authorize(request, TOKEN);

    assertThat(auth).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
    assertThat(auth.getPrincipal()).isEqualTo(authUserPrincipal());
    verify(objectMapper).readValue(TOKEN_CONTENT, OkapiAccessToken.class);
  }

  @Test
  void authorize_positive_pathWithPrefix() throws JsonProcessingException {
    var permissionsRequired = List.of(PERMISSION_1, PERMISSION_2);
    var modulePermissions = List.of(MODULE_PERMISSION_1, MODULE_PERMISSION_2);
    var routingEntry = new RoutingEntry().path(PATH).methods(List.of(METHOD)).permissionsRequired(permissionsRequired)
      .modulePermissions(modulePermissions);
    var moduleDescriptor = new ModuleDescriptor().id(MODULE_ID);

    when(request.getMethod()).thenReturn(METHOD);
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("/prefix");
    when(urlPathHelper.getPathWithinApplication(request)).thenReturn("/prefix" + PATH);
    when(routingEntryMatcher.lookup(METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(descriptorProvider.getModuleDescriptor()).thenReturn(moduleDescriptor);

    var auth = service.authorize(request, TOKEN);

    assertThat(auth).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
    assertThat(auth.getPrincipal()).isEqualTo(authUserPrincipal());
    verify(objectMapper).readValue(TOKEN_CONTENT, OkapiAccessToken.class);
  }

  @Test
  void authorize_positive_pathWithoutPrefixWhenItsDefinedInConfiguration() {
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("/prefix");
    when(urlPathHelper.getPathWithinApplication(request)).thenReturn(PATH);

    assertThrows(RoutingEntryMatchingException.class, () -> service.authorize(request, TOKEN));
  }

  @Test
  void authorize_negative_modAuthtokenThrowsForbiddenException() {
    var permissionsRequired = List.of(PERMISSION_1, PERMISSION_2);
    var modulePermissions = List.of(MODULE_PERMISSION_1, MODULE_PERMISSION_2);
    var routingEntry = new RoutingEntry().path(PATH).methods(List.of(METHOD)).permissionsRequired(permissionsRequired)
      .modulePermissions(modulePermissions);
    var moduleDescriptor = new ModuleDescriptor().id(MODULE_ID);
    var reqPerms = String.join(",", permissionsRequired);
    var modPerms = Map.of(MODULE_ID, modulePermissions);

    when(request.getMethod()).thenReturn(METHOD);
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("");
    when(urlPathHelper.getPathWithinApplication(request)).thenReturn(PATH);
    when(routingEntryMatcher.lookup(METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(descriptorProvider.getModuleDescriptor()).thenReturn(moduleDescriptor);
    doThrow(FeignException.Forbidden.class).when(client)
      .checkAuthToken(PATH, reqPerms, null, modPerms, TOKEN, SUPERTENANT_ID, null);

    assertThrows(ForbiddenException.class, () -> service.authorize(request, TOKEN));
  }

  @Test
  void authorize_negative_modAuthtokenThrowsUnauthorizedException() {
    var permissionsRequired = List.of(PERMISSION_1, PERMISSION_2);
    var modulePermissions = List.of(MODULE_PERMISSION_1, MODULE_PERMISSION_2);
    var routingEntry = new RoutingEntry().path(PATH).methods(List.of(METHOD)).permissionsRequired(permissionsRequired)
      .modulePermissions(modulePermissions);
    var moduleDescriptor = new ModuleDescriptor().id(MODULE_ID);
    var reqPerms = String.join(",", permissionsRequired);
    var modPerms = Map.of(MODULE_ID, modulePermissions);

    when(request.getMethod()).thenReturn(METHOD);
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("");
    when(urlPathHelper.getPathWithinApplication(request)).thenReturn(PATH);
    when(routingEntryMatcher.lookup(METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(descriptorProvider.getModuleDescriptor()).thenReturn(moduleDescriptor);
    doThrow(FeignException.Unauthorized.class).when(client)
      .checkAuthToken(PATH, reqPerms, null, modPerms, TOKEN, SUPERTENANT_ID, null);

    assertThrows(NotAuthorizedException.class, () -> service.authorize(request, TOKEN));
  }

  @Test
  void authorize_negative_modAuthtokenThrowsInternalError() {
    var permissionsRequired = List.of(PERMISSION_1, PERMISSION_2);
    var modulePermissions = List.of(MODULE_PERMISSION_1, MODULE_PERMISSION_2);
    var routingEntry = new RoutingEntry().path(PATH).methods(List.of(METHOD)).permissionsRequired(permissionsRequired)
      .modulePermissions(modulePermissions);
    var moduleDescriptor = new ModuleDescriptor().id(MODULE_ID);

    when(request.getMethod()).thenReturn(METHOD);
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("");
    when(urlPathHelper.getPathWithinApplication(request)).thenReturn(PATH);
    when(routingEntryMatcher.lookup(METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(descriptorProvider.getModuleDescriptor()).thenReturn(moduleDescriptor);
    doThrow(FeignException.InternalServerError.class).when(client)
      .checkAuthToken(eq(PATH), anyString(), eq(null), any(), eq(TOKEN), eq(SUPERTENANT_ID), eq(null));

    assertThrows(FeignException.InternalServerError.class, () -> service.authorize(request, TOKEN));
  }

  @Test
  void authorize_negative_modAuthokenNotAvailable() {
    var permissionsRequired = List.of(PERMISSION_1, PERMISSION_2);
    var modulePermissions = List.of(MODULE_PERMISSION_1, MODULE_PERMISSION_2);
    var routingEntry = new RoutingEntry().path(PATH).methods(List.of(METHOD)).permissionsRequired(permissionsRequired)
      .modulePermissions(modulePermissions);
    var moduleDescriptor = new ModuleDescriptor().id(MODULE_ID);

    when(request.getMethod()).thenReturn(METHOD);
    when(environment.getProperty(ROUTER_PREFIX_PROPERTY, "")).thenReturn("");
    when(urlPathHelper.getPathWithinApplication(request)).thenReturn(PATH);
    when(routingEntryMatcher.lookup(METHOD, PATH)).thenReturn(Optional.of(routingEntry));
    when(descriptorProvider.getModuleDescriptor()).thenReturn(moduleDescriptor);
    doThrow(RetryableException.class).when(client)
      .checkAuthToken(eq(PATH), anyString(), eq(null), any(), eq(TOKEN), eq(SUPERTENANT_ID), eq(null));

    assertThrows(RetryableException.class, () -> service.authorize(request, TOKEN));
  }

  private static AuthUserPrincipal authUserPrincipal() {
    return new AuthUserPrincipal().userId(USER_ID).authUserId(USER_ID.toString()).tenant("test_tenant");
  }
}
