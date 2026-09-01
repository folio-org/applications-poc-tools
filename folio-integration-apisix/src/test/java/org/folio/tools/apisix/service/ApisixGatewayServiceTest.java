package org.folio.tools.apisix.service;

import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.domain.model.error.Parameter;
import org.folio.common.gateway.ApiGatewayService;
import org.folio.common.gateway.exception.TenantRouteUpdateException;
import org.folio.common.gateway.model.GatewayServiceDefinition;
import org.folio.test.types.UnitTest;
import org.folio.tools.apisix.client.ApisixAdminClient;
import org.folio.tools.apisix.client.ApisixAdminClient.ApisixEntry;
import org.folio.tools.apisix.client.ApisixAdminClient.ApisixResultList;
import org.folio.tools.apisix.exception.ApisixIntegrationException;
import org.folio.tools.apisix.model.ApisixRoute;
import org.folio.tools.apisix.model.ApisixService;
import org.folio.tools.apisix.model.ApisixTimeout;
import org.folio.tools.apisix.model.ApisixUpstream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ApisixGatewayServiceTest {

  private static final String MOD_ID = "test-module-0.0.1";
  private static final String SERVICE_URL = "http://test-module:8080";
  private static final String TENANT_NAME = "test-tenant";
  private static final String ROUTE_ID_1 = "route-id-1";
  private static final String ROUTE_ID_2 = "route-id-2";

  @InjectMocks private ApisixGatewayService apisixGatewayService;
  @Mock private ApisixAdminClient apisixAdminClient;
  @Mock private ApisixRouteFactory apisixRouteFactory;
  @Mock private ApisixRouteTenantService apisixRouteTenantService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(apisixAdminClient, apisixRouteFactory, apisixRouteTenantService);
  }

  private static ApisixService apisixService() {
    return new ApisixService().id(MOD_ID).name(MOD_ID)
      .upstream(new ApisixUpstream().type("roundrobin").scheme("http")
        .nodes(Map.of("test-module:8080", 1)).passHost("pass"));
  }

  private static ApisixEntry<ApisixService> serviceEntry(ApisixService service) {
    return new ApisixEntry<>("/apisix/services/" + service.getId(), service);
  }

  private static ApisixRoute moduleRoute(String routeId) {
    return new ApisixRoute().id(routeId).name(routeId).serviceId(MOD_ID)
      .labels(Map.of("module", MOD_ID, "interface", "test1-2.0"))
      .vars(List.of(List.of("http_x_okapi_tenant", "~~", ".*")));
  }

  private static ApisixResultList<ApisixRoute> routePage(List<ApisixRoute> routes, int total) {
    var entries = routes.stream()
      .map(route -> new ApisixEntry<>("/apisix/routes/" + route.getId(), route))
      .toList();
    return new ApisixResultList<>(total, entries);
  }

  private static ApisixEntry<ApisixRoute> routeEntry(ApisixRoute route) {
    return new ApisixEntry<>("/apisix/routes/" + route.getId(), route);
  }

  private static ApisixRoute varsPatch(ApisixRoute route) {
    return new ApisixRoute().vars(route.getVars());
  }

  private static HttpClientErrorException notFound() {
    return HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null);
  }

  private static HttpServerErrorException serverError(String message) {
    return HttpServerErrorException.create(HttpStatus.INTERNAL_SERVER_ERROR, message, null, null, null);
  }

  @Test
  void service_positive_isApiGatewayService() {
    assertThat(apisixGatewayService).isInstanceOf(ApiGatewayService.class);
  }

  @Nested
  @DisplayName("upsertService")
  class UpsertService {

    @Test
    void positive_serviceCreatedWhenNotFound() {
      when(apisixAdminClient.getService(MOD_ID)).thenThrow(notFound());

      apisixGatewayService.upsertService(new GatewayServiceDefinition().name(MOD_ID).url(SERVICE_URL));

      verify(apisixAdminClient).upsertService(MOD_ID, apisixService());
    }

    @Test
    void positive_serviceAlreadyExists() {
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));

      apisixGatewayService.upsertService(new GatewayServiceDefinition().name(MOD_ID).url(SERVICE_URL));

      verify(apisixAdminClient, never()).upsertService(anyString(), any(ApisixService.class));
    }

    @Test
    void positive_serviceExistsButUrlChanged() {
      var existing = apisixService();
      existing.getUpstream().nodes(Map.of("old-host:8080", 1));
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(existing));

      apisixGatewayService.upsertService(new GatewayServiceDefinition().name(MOD_ID).url(SERVICE_URL));

      verify(apisixAdminClient).upsertService(MOD_ID, apisixService());
    }

    @Test
    void positive_timeoutsAndRetriesMapped() {
      when(apisixAdminClient.getService(MOD_ID)).thenThrow(notFound());

      apisixGatewayService.upsertService(new GatewayServiceDefinition().name(MOD_ID).url("https://test-module")
        .connectTimeout(60000).readTimeout(30000).writeTimeout(15000).retries(5));

      var expected = new ApisixService().id(MOD_ID).name(MOD_ID)
        .upstream(new ApisixUpstream().type("roundrobin").scheme("https")
          .nodes(Map.of("test-module:443", 1)).passHost("pass").retries(5)
          .timeout(new ApisixTimeout().connect(60.0).read(30.0).send(15.0)));
      verify(apisixAdminClient).upsertService(MOD_ID, expected);
    }

    @Test
    void positive_partialTimeoutsFilledWithDefaults() {
      when(apisixAdminClient.getService(MOD_ID)).thenThrow(notFound());

      apisixGatewayService.upsertService(new GatewayServiceDefinition().name(MOD_ID).url(SERVICE_URL)
        .readTimeout(360000));

      var expected = new ApisixService().id(MOD_ID).name(MOD_ID)
        .upstream(new ApisixUpstream().type("roundrobin").scheme("http")
          .nodes(Map.of("test-module:8080", 1)).passHost("pass")
          .timeout(new ApisixTimeout().connect(60.0).read(360.0).send(60.0)));
      verify(apisixAdminClient).upsertService(MOD_ID, expected);
    }

    @Test
    void negative_hostNotResolvable() {
      assertThatThrownBy(() -> apisixGatewayService.upsertService(
          new GatewayServiceDefinition().name(MOD_ID).url("http://test_module:8080")))
        .isInstanceOf(ApisixIntegrationException.class)
        .hasMessageContaining("host");
    }

    @Test
    void positive_pathCarryingUrlMappedToProxyRewrite() {
      when(apisixAdminClient.getService(MOD_ID)).thenThrow(notFound());

      apisixGatewayService.upsertService(
        new GatewayServiceDefinition().name(MOD_ID).url("http://test-module:8080/mod-foo"));

      var expected = new ApisixService().id(MOD_ID).name(MOD_ID)
        .plugins(Map.of("proxy-rewrite", Map.of("regex_uri", List.of("^(.*)", "/mod-foo$1"))))
        .upstream(new ApisixUpstream().type("roundrobin").scheme("http")
          .nodes(Map.of("test-module:8080", 1)).passHost("pass"));
      verify(apisixAdminClient).upsertService(MOD_ID, expected);
    }

    @Test
    void positive_pathChangeTriggersUpdate() {
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));

      apisixGatewayService.upsertService(
        new GatewayServiceDefinition().name(MOD_ID).url("http://test-module:8080/mod-foo"));

      var expected = new ApisixService().id(MOD_ID).name(MOD_ID)
        .plugins(Map.of("proxy-rewrite", Map.of("regex_uri", List.of("^(.*)", "/mod-foo$1"))))
        .upstream(new ApisixUpstream().type("roundrobin").scheme("http")
          .nodes(Map.of("test-module:8080", 1)).passHost("pass"));
      verify(apisixAdminClient).upsertService(MOD_ID, expected);
    }

    @Test
    void positive_longServiceNameFallsBackToHashedId() {
      var longName = StringUtils.repeat("a", 65);
      var hashedId = sha1Hex(longName);
      when(apisixAdminClient.getService(hashedId)).thenThrow(notFound());

      apisixGatewayService.upsertService(new GatewayServiceDefinition().name(longName).url(SERVICE_URL));

      var expected = new ApisixService().id(hashedId).name(longName)
        .upstream(new ApisixUpstream().type("roundrobin").scheme("http")
          .nodes(Map.of("test-module:8080", 1)).passHost("pass"));
      verify(apisixAdminClient).upsertService(hashedId, expected);
    }
  }

  @Nested
  @DisplayName("deleteService")
  class DeleteService {

    @Test
    void positive() {
      apisixGatewayService.deleteService(MOD_ID);

      verify(apisixAdminClient).deleteService(MOD_ID);
    }

    @Test
    void negative_deleteFailed() {
      doThrow(serverError("Delete failed")).when(apisixAdminClient).deleteService(MOD_ID);

      assertThatThrownBy(() -> apisixGatewayService.deleteService(MOD_ID))
        .isInstanceOf(ApisixIntegrationException.class)
        .hasMessage("Failed to delete APISIX service: " + MOD_ID)
        .extracting(exception -> ((ApisixIntegrationException) exception).getErrors())
        .satisfies(errors -> assertThat(errors).hasSize(1)
          .first().extracting(Parameter::getKey).isEqualTo("cause"));
    }
  }

  @Nested
  @DisplayName("deleteServiceRoutes")
  class DeleteServiceRoutes {

    @Test
    void positive() {
      var serviceRoute1 = moduleRoute(ROUTE_ID_1);
      var serviceRoute2 = moduleRoute(ROUTE_ID_2);
      var foreignRoute = new ApisixRoute().id("foreign-route").serviceId("other-service");
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500))
        .thenReturn(routePage(List.of(serviceRoute1, serviceRoute2, foreignRoute), 3));

      apisixGatewayService.deleteServiceRoutes(MOD_ID);

      verify(apisixAdminClient).deleteRoute(ROUTE_ID_1);
      verify(apisixAdminClient).deleteRoute(ROUTE_ID_2);
      verify(apisixAdminClient, never()).deleteRoute("foreign-route");
    }

    @Test
    void negative_serviceNotFound() {
      when(apisixAdminClient.getService(MOD_ID)).thenThrow(notFound());

      assertThatThrownBy(() -> apisixGatewayService.deleteServiceRoutes(MOD_ID))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("No such service: " + MOD_ID);
    }

    @Test
    void positive_routeAlreadyDeleted() {
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(List.of(moduleRoute(ROUTE_ID_1)), 1));
      doThrow(notFound()).when(apisixAdminClient).deleteRoute(ROUTE_ID_1);

      apisixGatewayService.deleteServiceRoutes(MOD_ID);

      verify(apisixAdminClient).deleteRoute(ROUTE_ID_1);
    }

    @Test
    void negative_deleteFailed() {
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(List.of(moduleRoute(ROUTE_ID_1)), 1));
      doThrow(serverError("Delete failed")).when(apisixAdminClient).deleteRoute(ROUTE_ID_1);

      assertThatThrownBy(() -> apisixGatewayService.deleteServiceRoutes(MOD_ID))
        .isInstanceOf(ApisixIntegrationException.class)
        .hasMessage("Failed to delete all routes for service " + MOD_ID);
    }
  }

  @Nested
  @DisplayName("addRoutes")
  class AddRoutes {

    @Test
    void positive() {
      var descriptor = new ModuleDescriptor().id(MOD_ID);
      var route1 = moduleRoute(ROUTE_ID_1);
      var route2 = moduleRoute(ROUTE_ID_2);
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixRouteFactory.createRoutes(descriptor, MOD_ID)).thenReturn(List.of(route1, route2));

      apisixGatewayService.addRoutes(List.of(descriptor));

      verify(apisixAdminClient).upsertRoute(ROUTE_ID_1, route1);
      verify(apisixAdminClient).upsertRoute(ROUTE_ID_2, route2);
    }

    @Test
    void negative_serviceNotFound() {
      var descriptor = new ModuleDescriptor().id(MOD_ID);
      when(apisixAdminClient.getService(MOD_ID)).thenThrow(notFound());

      assertThatThrownBy(() -> apisixGatewayService.addRoutes(List.of(descriptor)))
        .isInstanceOf(ApisixIntegrationException.class)
        .hasMessage("Failed to find APISIX service for module: " + MOD_ID);
    }

    @Test
    void negative_serviceLookupFailed() {
      var descriptor = new ModuleDescriptor().id(MOD_ID);
      var cause = serverError("Connection reset");
      when(apisixAdminClient.getService(MOD_ID)).thenThrow(cause);

      assertThatThrownBy(() -> apisixGatewayService.addRoutes(List.of(descriptor)))
        .isInstanceOf(ApisixIntegrationException.class)
        .hasMessage("Failed to find APISIX service for module: " + MOD_ID)
        .hasCause(cause);
    }

    @Test
    void negative_partialFailure() {
      var descriptor = new ModuleDescriptor().id(MOD_ID);
      var route1 = moduleRoute(ROUTE_ID_1);
      var route2 = moduleRoute(ROUTE_ID_2);
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixRouteFactory.createRoutes(descriptor, MOD_ID)).thenReturn(List.of(route1, route2));
      when(apisixAdminClient.upsertRoute(ROUTE_ID_1, route1)).thenReturn(routeEntry(route1));
      doThrow(serverError("Create failed")).when(apisixAdminClient).upsertRoute(ROUTE_ID_2, route2);

      assertThatThrownBy(() -> apisixGatewayService.addRoutes(List.of(descriptor)))
        .isInstanceOf(ApisixIntegrationException.class)
        .hasMessage("Failed to create routes")
        .extracting(exception -> ((ApisixIntegrationException) exception).getErrors())
        .satisfies(errors -> assertThat(errors).hasSize(1)
          .first().extracting(Parameter::getKey).isEqualTo(ROUTE_ID_2));

      verify(apisixAdminClient).upsertRoute(ROUTE_ID_1, route1);
    }
  }

  @Nested
  @DisplayName("updateRoutes")
  class UpdateRoutes {

    @Test
    void positive_neverRePutsExistingRoutes() {
      var descriptor = new ModuleDescriptor().id(MOD_ID);
      var existingKept = moduleRoute(ROUTE_ID_1);
      var existingStale = moduleRoute("stale-route-id");
      var desiredKept = moduleRoute(ROUTE_ID_1);
      var desiredNew = moduleRoute(ROUTE_ID_2);
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(List.of(existingKept, existingStale), 2));
      when(apisixRouteFactory.createRoutes(descriptor, MOD_ID)).thenReturn(List.of(desiredKept, desiredNew));

      apisixGatewayService.updateRoutes(List.of(descriptor));

      verify(apisixAdminClient).upsertRoute(ROUTE_ID_2, desiredNew);
      verify(apisixAdminClient).deleteRoute("stale-route-id");
      verify(apisixAdminClient, never()).upsertRoute(ROUTE_ID_1, desiredKept);
    }

    @Test
    void negative_partialFailure() {
      var descriptor = new ModuleDescriptor().id(MOD_ID);
      var desiredNew = moduleRoute(ROUTE_ID_2);
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(List.of(), 0));
      when(apisixRouteFactory.createRoutes(descriptor, MOD_ID)).thenReturn(List.of(desiredNew));
      doThrow(serverError("Create failed")).when(apisixAdminClient).upsertRoute(ROUTE_ID_2, desiredNew);

      assertThatThrownBy(() -> apisixGatewayService.updateRoutes(List.of(descriptor)))
        .isInstanceOf(ApisixIntegrationException.class)
        .hasMessage("Failed to update routes");
    }
  }

  @Nested
  @DisplayName("removeRoutes")
  class RemoveRoutes {

    @Test
    void positive() {
      var descriptor = new ModuleDescriptor().id(MOD_ID);
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500))
        .thenReturn(routePage(List.of(moduleRoute(ROUTE_ID_1), moduleRoute(ROUTE_ID_2)), 2));

      apisixGatewayService.removeRoutes(List.of(descriptor));

      verify(apisixAdminClient).deleteRoute(ROUTE_ID_1);
      verify(apisixAdminClient).deleteRoute(ROUTE_ID_2);
    }
  }

  @Nested
  @DisplayName("addTenantToModuleRoutes")
  class AddTenantToModuleRoutes {

    @Test
    void positive() {
      var route1 = moduleRoute(ROUTE_ID_1);
      var route2 = moduleRoute(ROUTE_ID_2);
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(List.of(route1, route2), 2));
      when(apisixRouteTenantService.addTenant(route1, TENANT_NAME)).thenReturn(true);
      when(apisixRouteTenantService.addTenant(route2, TENANT_NAME)).thenReturn(true);

      apisixGatewayService.addTenantToModuleRoutes(MOD_ID, TENANT_NAME);

      verify(apisixAdminClient).patchRoute(ROUTE_ID_1, varsPatch(route1));
      verify(apisixAdminClient).patchRoute(ROUTE_ID_2, varsPatch(route2));
    }

    @Test
    void positive_skipsUnchangedRoutes() {
      var route1 = moduleRoute(ROUTE_ID_1);
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(List.of(route1), 1));
      when(apisixRouteTenantService.addTenant(route1, TENANT_NAME)).thenReturn(false);

      apisixGatewayService.addTenantToModuleRoutes(MOD_ID, TENANT_NAME);

      verify(apisixAdminClient, never()).patchRoute(anyString(), any(ApisixRoute.class));
    }

    @Test
    void positive_noRoutesFound() {
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(List.of(), 0));

      apisixGatewayService.addTenantToModuleRoutes(MOD_ID, TENANT_NAME);

      verify(apisixAdminClient, never()).patchRoute(anyString(), any(ApisixRoute.class));
    }

    @Test
    void negative_blankTenant() {
      assertThatThrownBy(() -> apisixGatewayService.addTenantToModuleRoutes(MOD_ID, " "))
        .isInstanceOf(TenantRouteUpdateException.class)
        .hasMessageContaining("Failed to add tenant");
    }

    @Test
    void negative_partialFailure() {
      var route1 = moduleRoute(ROUTE_ID_1);
      var route2 = moduleRoute(ROUTE_ID_2);
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(List.of(route1, route2), 2));
      when(apisixRouteTenantService.addTenant(route1, TENANT_NAME)).thenReturn(true);
      when(apisixRouteTenantService.addTenant(route2, TENANT_NAME)).thenReturn(true);
      when(apisixAdminClient.patchRoute(ROUTE_ID_1, varsPatch(route1))).thenReturn(routeEntry(route1));
      doThrow(serverError("Update failed")).when(apisixAdminClient).patchRoute(ROUTE_ID_2, varsPatch(route2));

      assertThatThrownBy(() -> apisixGatewayService.addTenantToModuleRoutes(MOD_ID, TENANT_NAME))
        .isInstanceOf(TenantRouteUpdateException.class)
        .hasMessageContaining("Failed to add tenant")
        .hasMessageContaining(ROUTE_ID_2);

      verify(apisixAdminClient).patchRoute(ROUTE_ID_1, varsPatch(route1));
    }
  }

  @Nested
  @DisplayName("removeTenantFromModuleRoutes")
  class RemoveTenantFromModuleRoutes {

    @Test
    void positive() {
      var route1 = moduleRoute(ROUTE_ID_1);
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(List.of(route1), 1));
      when(apisixRouteTenantService.removeTenant(route1, TENANT_NAME)).thenReturn(true);

      apisixGatewayService.removeTenantFromModuleRoutes(MOD_ID, TENANT_NAME);

      verify(apisixAdminClient).patchRoute(ROUTE_ID_1, varsPatch(route1));
    }

    @Test
    void negative_partialFailure() {
      var route1 = moduleRoute(ROUTE_ID_1);
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(List.of(route1), 1));
      when(apisixRouteTenantService.removeTenant(route1, TENANT_NAME)).thenReturn(true);
      doThrow(serverError("Update failed")).when(apisixAdminClient).patchRoute(ROUTE_ID_1, varsPatch(route1));

      assertThatThrownBy(() -> apisixGatewayService.removeTenantFromModuleRoutes(MOD_ID, TENANT_NAME))
        .isInstanceOf(TenantRouteUpdateException.class)
        .hasMessageContaining("Failed to remove tenant")
        .hasMessageContaining(ROUTE_ID_1);
    }
  }

  @Nested
  @DisplayName("pagination")
  class Pagination {

    @Test
    void positive_multiplePagesScanned() {
      var routesFirstPage = IntStream.range(0, 500)
        .mapToObj(i -> moduleRoute("route-" + i))
        .toList();
      var lastRoute = moduleRoute("route-500");
      when(apisixAdminClient.getService(MOD_ID)).thenReturn(serviceEntry(apisixService()));
      when(apisixAdminClient.getRoutes(1, 500)).thenReturn(routePage(routesFirstPage, 501));
      when(apisixAdminClient.getRoutes(2, 500)).thenReturn(routePage(List.of(lastRoute), 501));

      apisixGatewayService.removeRoutes(List.of(new ModuleDescriptor().id(MOD_ID)));

      verify(apisixAdminClient, times(501)).deleteRoute(anyString());
    }
  }
}
