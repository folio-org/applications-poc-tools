package org.folio.security.integration.authtoken.service;

import static org.folio.common.utils.OkapiHeaders.SUPERTENANT_ID;
import static org.folio.security.service.AbstractAuthorizationService.ROUTER_PREFIX_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.RetryableException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.domain.model.RoutingEntry;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.util.UrlPathHelper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class OkapiAuthorizationServiceTest {

  private static final String TOKEN = "token";
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

  @InjectMocks private OkapiAuthorizationService service;

  @BeforeEach
  void setup() {
    service.setEnvironment(environment);
  }

  @Test
  void authorize_positive() {
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

    assertEquals(PreAuthenticatedAuthenticationToken.class, auth.getClass());
  }

  @Test
  void authorize_positive_pathWithPrefix() {
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

    assertEquals(PreAuthenticatedAuthenticationToken.class, auth.getClass());
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
}
