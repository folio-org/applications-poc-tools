package org.folio.tools.kong.service;

import static feign.Request.HttpMethod.GET;
import static feign.Request.HttpMethod.PUT;
import static feign.Request.create;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.common.domain.model.InterfaceDescriptor.SYSTEM_INTERFACE_TYPE;
import static org.folio.common.domain.model.InterfaceDescriptor.TIMER_INTERFACE;
import static org.folio.common.utils.OkapiHeaders.MODULE_ID;
import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.folio.common.utils.UuidUtils.randomId;
import static org.folio.tools.kong.service.KongGatewayServiceTest.TestValues.kongService;
import static org.folio.tools.kong.service.KongGatewayServiceTest.TestValues.mdWithMultipleInterface1;
import static org.folio.tools.kong.service.KongGatewayServiceTest.TestValues.mdWithMultipleInterface2;
import static org.folio.tools.kong.service.KongGatewayServiceTest.TestValues.mdWithTimerInterface;
import static org.folio.tools.kong.service.KongGatewayServiceTest.TestValues.moduleDescriptor;
import static org.folio.tools.kong.service.KongGatewayServiceTest.TestValues.multipleTypeHeaders;
import static org.folio.tools.kong.service.KongGatewayServiceTest.TestValues.route;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import feign.FeignException.InternalServerError;
import feign.FeignException.NotFound;
import feign.RequestTemplate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.folio.common.domain.model.InterfaceDescriptor;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.common.domain.model.error.Parameter;
import org.folio.test.types.UnitTest;
import org.folio.tools.kong.client.KongAdminClient;
import org.folio.tools.kong.client.KongAdminClient.KongResultList;
import org.folio.tools.kong.exception.KongIntegrationException;
import org.folio.tools.kong.exception.TenantRouteUpdateException;
import org.folio.tools.kong.model.Identifier;
import org.folio.tools.kong.model.Route;
import org.folio.tools.kong.model.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KongGatewayServiceTest {

  private static final String MOD_ID = "test-module-0.0.1";
  private static final String SERVICE_ID = randomId();
  private static final String SERVICE_URL = "https://mod-foo:443";
  private static final String ROUTE_TAGS = MOD_ID;

  @InjectMocks private KongGatewayService kongGatewayService;
  @Mock private KongAdminClient kongAdminClient;
  @Mock private KongRouteTenantService kongRouteTenantService;
  @Captor private ArgumentCaptor<Route> routeCaptor;

  @BeforeEach
  void setUp() {
    Configurator.setLevel(KongGatewayService.class, Level.DEBUG);
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(kongAdminClient, kongRouteTenantService);
  }

  @Nested
  @DisplayName("addRoutes")
  class AddRoutes {

    @Test
    void positive() {
      var serviceId = UUID.randomUUID().toString();
      when(kongAdminClient.getService(MOD_ID)).thenReturn(new Service().id(serviceId).name(MOD_ID));
      when(kongAdminClient.upsertRoute(eq(serviceId), anyString(), routeCaptor.capture())).then(i -> i.getArgument(2));

      kongGatewayService.addRoutes(singletonList(moduleDescriptor()));

      assertThat(routeCaptor.getAllValues()).hasSize(5).isEqualTo(List.of(
        route(List.of("GET"), "^/entities/([^/]+)$", "test1-2.0"),
        route(List.of("PUT"), "^/entities/([^/]+)/sub-entities$", "test1-2.0"),
        route(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE"),
          "^/entities/sub-entities(.*)$", 0, "test1-2.0", MOD_ID),
        route(List.of("PUT"), "/tests/1", 1, "test1-2.0", MOD_ID),
        route(List.of("GET"), "/test2-entities", 1, "test2-1.0", MOD_ID)));
    }

    @Test
    void positive_interfaceTypeMultiple() {
      var fooModuleId = "mod-foo-1.0.0";
      var fooModuleUuid = UUID.randomUUID().toString();
      var barModuleId = "mod-bar-1.0.0";
      var barModuleUuid = UUID.randomUUID().toString();
      when(kongAdminClient.getService(fooModuleId)).thenReturn(new Service().id(fooModuleUuid).name(fooModuleId));
      when(kongAdminClient.getService(barModuleId)).thenReturn(new Service().id(barModuleUuid).name(barModuleId));
      when(kongAdminClient.upsertRoute(anyString(), anyString(), routeCaptor.capture())).then(i -> i.getArgument(2));

      kongGatewayService.addRoutes(List.of(mdWithMultipleInterface1(), mdWithMultipleInterface2()));

      assertThat(routeCaptor.getAllValues()).hasSize(4).isEqualTo(List.of(
        route(List.of("GET"), "/baz/entities", 1, "baz-multiple-1.0", fooModuleId, multipleTypeHeaders(fooModuleId)),
        route(List.of("POST"), "/foo/entities", 1, "foo-1.0", fooModuleId),
        route(List.of("GET"), "/baz/entities", 1, "baz-multiple-1.0", barModuleId, multipleTypeHeaders(barModuleId)),
        route(List.of("POST"), "/bar/entities", 1, "bar-1.0", barModuleId)));
    }

    @Test
    void positive_timerInterfaceIgnored() {
      var serviceId = UUID.randomUUID().toString();
      when(kongAdminClient.getService(MOD_ID)).thenReturn(new Service().id(serviceId).name(MOD_ID));
      when(kongAdminClient.upsertRoute(eq(serviceId), anyString(), routeCaptor.capture())).then(i -> i.getArgument(2));

      kongGatewayService.addRoutes(singletonList(mdWithTimerInterface()));

      assertThat(routeCaptor.getAllValues()).hasSize(5).isEqualTo(List.of(
        route(List.of("GET"), "^/entities/([^/]+)$", "test1-2.0"),
        route(List.of("PUT"), "^/entities/([^/]+)/sub-entities$", "test1-2.0"),
        route(List.of("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE"),
          "^/entities/sub-entities(.*)$", 0, "test1-2.0", MOD_ID),
        route(List.of("PUT"), "/tests/1", 1, "test1-2.0", MOD_ID),
        route(List.of("GET"), "/test2-entities", 1, "test2-1.0", MOD_ID)));
    }

    @Test
    void negative_failedToUpsert() {
      var serviceId = UUID.randomUUID().toString();
      var kongRoute = route(List.of("GET"), "^/entities/([^/]+)$", "test-1.0");
      var routeName = kongRoute.getName();
      var request = create(PUT, "/services/" + serviceId + "/" + routeName, emptyMap(), null, (RequestTemplate) null);
      var internalServerError = new InternalServerError("Failed to create route", request, null, emptyMap());

      when(kongAdminClient.getService(MOD_ID)).thenReturn(new Service().id(serviceId).name(MOD_ID));
      when(kongAdminClient.upsertRoute(serviceId, routeName, kongRoute)).thenThrow(internalServerError);

      var routingEntry = new RoutingEntry().methods(List.of("GET")).pathPattern("/entities/{id}");
      var interfaceDesc = new InterfaceDescriptor().id("test").version("1.0").handlers(List.of(routingEntry));
      var moduleDescriptor = new ModuleDescriptor().id(MOD_ID).provides(List.of(interfaceDesc));

      var moduleDescriptors = List.of(moduleDescriptor);
      assertThatThrownBy(() -> kongGatewayService.addRoutes(moduleDescriptors))
        .isInstanceOf(KongIntegrationException.class)
        .hasMessage("Failed to create routes")
        .satisfies(error -> assertThat(((KongIntegrationException) error).getErrors()).isEqualTo(
          List.of(new Parameter()
            .key("RoutingEntry(methods=[GET], pathPattern=/entities/{id}, path=null)")
            .value("Failed to create route"))));
    }

    @Test
    void negative_serviceNotFound() {
      var request = create(GET, "/services/" + MOD_ID, emptyMap(), null, (RequestTemplate) null);
      when(kongAdminClient.getService(MOD_ID)).thenThrow(new NotFound("Not found", request, null, emptyMap()));

      var moduleDescriptors = List.of(moduleDescriptor());
      assertThatThrownBy(() -> kongGatewayService.addRoutes(moduleDescriptors))
        .isInstanceOf(KongIntegrationException.class)
        .hasMessage("Failed to find Kong service for module: test-module-0.0.1")
        .satisfies(error -> assertThat(((KongIntegrationException) error).getErrors()).isEqualTo(List.of(
          new Parameter().key("test-module-0.0.1").value("Service is not found"))));

      verify(kongAdminClient, never()).upsertRoute(anyString(), anyString(), any(Route.class));
    }
  }

  @Nested
  @DisplayName("removeRoutes")
  class RemoveRoutes {

    @Test
    void positive() {
      var kongRoute = new Route().id("routeId").service(Identifier.of(SERVICE_ID));
      var routesByTag = new KongResultList<>(null, List.of(kongRoute));
      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null)).thenReturn(routesByTag);

      kongGatewayService.removeRoutes(List.of(moduleDescriptor()));

      verify(kongAdminClient).deleteRoute(SERVICE_ID, "routeId");
    }

    @Test
    void positive_twoPages() {
      var kongRoute1 = new Route().id("routeId1").service(Identifier.of(SERVICE_ID));
      var kongRoute2 = new Route().id("routeId2").service(Identifier.of(SERVICE_ID));
      var routesByTag1 = new KongResultList<>("test-offset", List.of(kongRoute1));
      var routesByTag2 = new KongResultList<>(null, List.of(kongRoute2));

      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null)).thenReturn(routesByTag1);
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, "test-offset")).thenReturn(routesByTag2);

      kongGatewayService.removeRoutes(List.of(moduleDescriptor()));

      verify(kongAdminClient).deleteRoute(SERVICE_ID, "routeId1");
      verify(kongAdminClient).deleteRoute(SERVICE_ID, "routeId2");
    }

    @Test
    void negative_failedToFindRoutes() {
      var url = format("/routes?tags=%s", MOD_ID);
      var request = create(PUT, url, emptyMap(), null, (RequestTemplate) null);
      var internalServerError = new InternalServerError("Unknown error", request, null, emptyMap());

      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null)).thenThrow(internalServerError);

      var moduleDescriptors = List.of(moduleDescriptor());
      assertThatThrownBy(() -> kongGatewayService.removeRoutes(moduleDescriptors))
        .isInstanceOf(KongIntegrationException.class)
        .hasMessage("Failed to load routes")
        .satisfies(error -> assertThat(((KongIntegrationException) error).getErrors()).isEqualTo(List.of(
          new Parameter().key("Failed to find routes").value("Unknown error"))));
    }

    @Test
    void negative_serviceNotFound() {
      var request = create(PUT, "/services/" + MOD_ID, emptyMap(), null, (RequestTemplate) null);
      var notFoundError = new NotFound("Not Found", request, null, emptyMap());
      when(kongAdminClient.getService(MOD_ID)).thenThrow(notFoundError);

      var moduleDescriptors = List.of(moduleDescriptor());
      assertThatThrownBy(() -> kongGatewayService.removeRoutes(moduleDescriptors))
        .isInstanceOf(KongIntegrationException.class)
        .hasMessage("Failed to find Kong service for module: test-module-0.0.1")
        .satisfies(error -> assertThat(((KongIntegrationException) error).getErrors()).isEqualTo(List.of(
          new Parameter().key("test-module-0.0.1").value("Service is not found"))));
    }

    @Test
    void negative_failedToDeleteRoutes() {
      var kongRoute = new Route().id("routeId").service(Identifier.of(SERVICE_ID));
      var routesByTag = new KongResultList<>(null, List.of(kongRoute));
      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null)).thenReturn(routesByTag);

      var url = format("/routes?tags=%s", MOD_ID);
      var request = create(PUT, url, emptyMap(), null, (RequestTemplate) null);
      var internalServerError = new InternalServerError("Failed to create route", request, null, emptyMap());
      doThrow(internalServerError).when(kongAdminClient).deleteRoute(SERVICE_ID, "routeId");

      var moduleDescriptors = List.of(moduleDescriptor());
      assertThatThrownBy(() -> kongGatewayService.removeRoutes(moduleDescriptors))
        .isInstanceOf(KongIntegrationException.class)
        .hasMessage("Failed to remove routes")
        .satisfies(error -> assertThat(((KongIntegrationException) error).getErrors()).isEqualTo(List.of(
          new Parameter().key("routeId").value("Failed to create route"))));
    }
  }

  @Nested
  @DisplayName("updateRoutes")
  class UpdateRoutes {

    private static final String INTERFACE_NAME = "test-interface";
    private static final String INTERFACE_VERSION = "1.0";
    private static final String INTERFACE_ID = INTERFACE_NAME + "-" + INTERFACE_VERSION;

    @Test
    void positive_allRoutesExists() {
      var existingRoutes = List.of(
        route(List.of("POST"), "/entities", 1, INTERFACE_ID, MOD_ID, emptyMap()),
        route(List.of("GET"), "^/entities/([^/]+)$", 0, INTERFACE_ID, MOD_ID, emptyMap()));

      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(MOD_ID, null)).thenReturn(new KongResultList<>(null, existingRoutes));

      kongGatewayService.updateRoutes(List.of(moduleDescriptor()));

      verifyNoMoreInteractions(kongAdminClient);
    }

    @Test
    void positive_noRoutesFound() {
      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(MOD_ID, null)).thenReturn(new KongResultList<>(null, emptyList()));
      when(kongAdminClient.upsertRoute(anyString(), anyString(), routeCaptor.capture())).then(i -> i.getArgument(2));

      kongGatewayService.updateRoutes(List.of(moduleDescriptor()));

      assertThat(routeCaptor.getAllValues()).hasSize(2).isEqualTo(List.of(
        route(List.of("POST"), "/entities", 1, INTERFACE_ID, MOD_ID, emptyMap()),
        route(List.of("GET"), "^/entities/([^/]+)$", 0, INTERFACE_ID, MOD_ID, emptyMap())));
    }

    @Test
    void positive_deprecatedRoutesFound() {
      var deprecatedRoute = route(List.of("PUT"), "^/entities/([^/]+)$", 0, INTERFACE_ID, MOD_ID, emptyMap());
      var routeToKeep = route(List.of("POST"), "/entities", 1, INTERFACE_ID, MOD_ID, emptyMap());
      var existingRoutes = List.of(routeToKeep, deprecatedRoute);

      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(MOD_ID, null)).thenReturn(new KongResultList<>(null, existingRoutes));
      when(kongAdminClient.upsertRoute(anyString(), anyString(), routeCaptor.capture())).then(i -> i.getArgument(2));

      kongGatewayService.updateRoutes(List.of(moduleDescriptor()));

      assertThat(routeCaptor.getAllValues()).hasSize(1).isEqualTo(List.of(
        route(List.of("GET"), "^/entities/([^/]+)$", 0, INTERFACE_ID, MOD_ID, emptyMap())));
      verify(kongAdminClient).deleteRoute(SERVICE_ID, deprecatedRoute.getName());
    }

    @Test
    void positive_serviceNotFound() {
      var request = create(GET, "/services/" + MOD_ID, emptyMap(), null, (RequestTemplate) null);
      when(kongAdminClient.getService(MOD_ID)).thenThrow(new NotFound("Not found", request, null, emptyMap()));

      var moduleDescriptors = List.of(moduleDescriptor());
      assertThatThrownBy(() -> kongGatewayService.addRoutes(moduleDescriptors))
        .isInstanceOf(KongIntegrationException.class)
        .hasMessage("Failed to find Kong service for module: test-module-0.0.1")
        .satisfies(error -> assertThat(((KongIntegrationException) error).getErrors()).isEqualTo(List.of(
          new Parameter().key("test-module-0.0.1").value("Service is not found"))));

      verify(kongAdminClient, never()).upsertRoute(anyString(), anyString(), any(Route.class));
      verify(kongAdminClient, never()).deleteRoute(anyString(), anyString());
    }

    @Test
    void positive_mgrComponent() {
      var serviceId = UUID.randomUUID().toString();
      var moduleId = "mgr-component-1.0.0";
      when(kongAdminClient.getService(moduleId)).thenReturn(new Service().id(serviceId).name(moduleId));
      when(kongAdminClient.upsertRoute(eq(serviceId), anyString(), routeCaptor.capture())).then(i -> i.getArgument(2));

      var moduleDescriptor = moduleDescriptor().id(moduleId);

      kongGatewayService.addRoutes(singletonList(moduleDescriptor));

      var capturedRoutes = routeCaptor.getAllValues();
      assertThat(capturedRoutes).isNotEmpty();

      capturedRoutes.forEach(route -> {
        assertThat(route.getExpression()).doesNotContain("http.headers.x_okapi_tenant");
      });
    }

    static ModuleDescriptor moduleDescriptor() {
      return new ModuleDescriptor()
        .id(MOD_ID)
        .provides(List.of(
          new InterfaceDescriptor().id(INTERFACE_NAME).version(INTERFACE_VERSION).handlers(List.of(
            new RoutingEntry().methods(List.of("POST")).pathPattern("/entities"),
            new RoutingEntry().methods(List.of("GET")).pathPattern("/entities/{id}")))
        ));
    }
  }

  @Nested
  @DisplayName("upsertService")
  class UpsertService {

    @Test
    void positive() {
      var kongService = kongService();
      when(kongAdminClient.getService(MOD_ID)).thenThrow(NotFound.class);

      kongGatewayService.upsertService(kongService);

      verify(kongAdminClient).upsertService(MOD_ID, kongService);
    }

    @Test
    void positive_serviceAlreadyExists() {
      var kongService = kongService();
      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService);

      kongGatewayService.upsertService(kongService);

      verify(kongAdminClient, never()).upsertService(anyString(), any(Service.class));
    }

    @Test
    void positive_serviceExistsButUrlChanged() {
      var kongService = kongService();
      var existingService = new Service().protocol("https").host("mod-foo-old").port(443);
      when(kongAdminClient.getService(MOD_ID)).thenReturn(existingService);

      kongGatewayService.upsertService(kongService);

      verify(kongAdminClient).upsertService(MOD_ID, kongService);
    }

    @Test
    void positive_serviceExistsButUrlChanged2() {
      var kongService = kongService();
      var existingService = new Service().url("https://mod-foo-old:443");
      when(kongAdminClient.getService(MOD_ID)).thenReturn(existingService);

      kongGatewayService.upsertService(kongService);

      verify(kongAdminClient).upsertService(MOD_ID, kongService);
    }
  }

  @Nested
  @DisplayName("deleteService")
  class DeleteService {

    @Test
    void positive() {
      kongGatewayService.deleteService(MOD_ID);
      verify(kongAdminClient).deleteService(MOD_ID);
    }

    @Test
    void negative_serviceIsNotFound() {
      var request = create(GET, "/services/" + MOD_ID, emptyMap(), null, (RequestTemplate) null);
      var errorMessage = "Service is not found: test-module-0.0.1";
      var notFoundException = new NotFound(errorMessage, request, null, emptyMap());

      doThrow(notFoundException).when(kongAdminClient).deleteService(MOD_ID);

      assertThatThrownBy(() -> kongGatewayService.deleteService(MOD_ID))
        .isInstanceOf(KongIntegrationException.class)
        .hasMessage("Failed to delete Kong service: test-module-0.0.1")
        .satisfies(error -> assertThat(((KongIntegrationException) error).getErrors())
          .isEqualTo(List.of(new Parameter().key("cause").value(errorMessage))));
    }

    @Test
    void positive_deleteServiceRoutes() {
      var mockRoutes =
        KongResultList.<Route>builder().data(List.of(new Route().id("R1"), new Route().id("R2"))).build();
      var count = new AtomicInteger(0);
      when(kongAdminClient.getServiceRoutes(MOD_ID, null)).thenAnswer(
        inv -> count.getAndIncrement() > 0 ? new KongResultList<Route>() : mockRoutes);
      kongGatewayService.deleteServiceRoutes(MOD_ID);
      verify(kongAdminClient, times(1)).deleteRoute(MOD_ID, "R1");
      verify(kongAdminClient, times(1)).deleteRoute(MOD_ID, "R2");
    }

    @Test
    void negative_deleteServiceRoutes_error() {
      var cause = new RuntimeException("Test");
      when(kongAdminClient.getServiceRoutes(MOD_ID, null)).thenThrow(cause);
      assertThatThrownBy(() -> kongGatewayService.deleteServiceRoutes(MOD_ID)).isInstanceOf(
          KongIntegrationException.class).hasCause(cause)
        .hasMessage("Failed to delete all routes for service " + MOD_ID);
    }
  }

  @Nested
  @DisplayName("addTenantToModuleRoutes")
  class AddTenantToModuleRoutes {

    private static final String TENANT_NAME = "test-tenant";
    private static final String ROUTE_ID_1 = "route-1";
    private static final String ROUTE_ID_2 = "route-2";

    @Test
    @DisplayName("should add tenant to all module routes successfully")
    void addTenantToModuleRoutes_positive() {
      var route1 = new Route().id(ROUTE_ID_1).name("route-name-1")
        .expression("http.path == \"/test1\"");
      var route2 = new Route().id(ROUTE_ID_2).name("route-name-2")
        .expression("http.path == \"/test2\"");
      var routes = List.of(route1, route2);

      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null))
        .thenReturn(new KongResultList<>(null, routes));
      when(kongAdminClient.upsertRoute(anyString(), eq("route-name-1"), any(Route.class))).thenReturn(route1);
      when(kongAdminClient.upsertRoute(anyString(), eq("route-name-2"), any(Route.class))).thenReturn(route2);
      when(kongRouteTenantService.addTenant(route1, TENANT_NAME)).thenReturn(route1);
      when(kongRouteTenantService.addTenant(route2, TENANT_NAME)).thenReturn(route2);

      kongGatewayService.addTenantToModuleRoutes(MOD_ID, TENANT_NAME);

      verify(kongAdminClient).getService(MOD_ID);
      verify(kongAdminClient).getRoutesByTag(ROUTE_TAGS, null);
      verify(kongRouteTenantService).addTenant(route1, TENANT_NAME);
      verify(kongRouteTenantService).addTenant(route2, TENANT_NAME);
      verify(kongAdminClient, times(2)).upsertRoute(anyString(), anyString(), any(Route.class));
    }

    @Test
    @DisplayName("should handle empty routes list")
    void addTenantToModuleRoutes_emptyRoutes() {
      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null))
        .thenReturn(new KongResultList<>(null, emptyList()));

      kongGatewayService.addTenantToModuleRoutes(MOD_ID, TENANT_NAME);

      verify(kongAdminClient).getService(MOD_ID);
      verify(kongAdminClient).getRoutesByTag(ROUTE_TAGS, null);
      verify(kongRouteTenantService, never()).addTenant(any(), any());
      verify(kongAdminClient, never()).upsertRoute(anyString(), anyString(), any(Route.class));
    }

    @Test
    @DisplayName("should throw exception when service not found")
    void addTenantToModuleRoutes_serviceNotFound() {
      var request = create(GET, "/services/" + MOD_ID, emptyMap(), null, (RequestTemplate) null);
      when(kongAdminClient.getService(MOD_ID))
        .thenThrow(new NotFound("Service not found", request, null, emptyMap()));

      assertThatThrownBy(() -> kongGatewayService.addTenantToModuleRoutes(MOD_ID, TENANT_NAME))
        .isInstanceOf(TenantRouteUpdateException.class)
        .hasMessageContaining("Failed to add tenant")
        .hasMessageContaining(TENANT_NAME)
        .hasMessageContaining(MOD_ID);

      verify(kongAdminClient).getService(MOD_ID);
    }

    @Test
    @DisplayName("should throw exception when route update fails")
    void addTenantToModuleRoutes_routeUpdateFails() {
      var route1 = new Route().id(ROUTE_ID_1).name("route-name-1")
        .expression("http.path == \"/test1\"");
      var routes = List.of(route1);
      var request = create(PUT, "/services/" + SERVICE_ID + "/routes/route-name-1", emptyMap(), null, 
        (RequestTemplate) null);

      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null))
        .thenReturn(new KongResultList<>(null, routes));
      when(kongRouteTenantService.addTenant(route1, TENANT_NAME)).thenReturn(route1);
      doThrow(new InternalServerError("Update failed", request, null, emptyMap()))
        .when(kongAdminClient).upsertRoute(anyString(), anyString(), any(Route.class));

      assertThatThrownBy(() -> kongGatewayService.addTenantToModuleRoutes(MOD_ID, TENANT_NAME))
        .isInstanceOf(TenantRouteUpdateException.class)
        .hasMessageContaining("Failed to add tenant")
        .hasMessageContaining(TENANT_NAME)
        .hasMessageContaining(MOD_ID);

      verify(kongAdminClient).getService(MOD_ID);
      verify(kongAdminClient).getRoutesByTag(ROUTE_TAGS, null);
      verify(kongRouteTenantService).addTenant(route1, TENANT_NAME);
      verify(kongAdminClient).upsertRoute(anyString(), anyString(), any(Route.class));
    }

    @Test
    @DisplayName("should throw exception when partial routes update fails")
    void addTenantToModuleRoutes_partialFailure() {
      var route1 = new Route().id(ROUTE_ID_1).name("route-name-1")
        .expression("http.path == \"/test1\"");
      var route2 = new Route().id(ROUTE_ID_2).name("route-name-2")
        .expression("http.path == \"/test2\"");
      var routes = List.of(route1, route2);
      var request = create(PUT, "/services/" + SERVICE_ID + "/routes/route-name-2", emptyMap(), null, 
        (RequestTemplate) null);

      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null))
        .thenReturn(new KongResultList<>(null, routes));
      when(kongAdminClient.upsertRoute(anyString(), eq("route-name-1"), any(Route.class))).thenReturn(route1);
      when(kongRouteTenantService.addTenant(route1, TENANT_NAME)).thenReturn(route1);
      when(kongRouteTenantService.addTenant(route2, TENANT_NAME)).thenReturn(route2);
      doThrow(new InternalServerError("Update failed", request, null, emptyMap()))
        .when(kongAdminClient).upsertRoute(anyString(), eq("route-name-2"), any(Route.class));

      assertThatThrownBy(() -> kongGatewayService.addTenantToModuleRoutes(MOD_ID, TENANT_NAME))
        .isInstanceOf(TenantRouteUpdateException.class)
        .hasMessageContaining("Failed to add tenant")
        .hasMessageContaining("route-name-2");

      verify(kongAdminClient).getService(MOD_ID);
      verify(kongAdminClient).getRoutesByTag(ROUTE_TAGS, null);
      verify(kongRouteTenantService, times(2)).addTenant(any(), eq(TENANT_NAME));
      verify(kongAdminClient, times(2)).upsertRoute(anyString(), anyString(), any(Route.class));
    }
  }

  @Nested
  @DisplayName("removeTenantFromModuleRoutes")
  class RemoveTenantFromModuleRoutes {

    private static final String TENANT_NAME = "test-tenant";
    private static final String ROUTE_ID_1 = "route-1";
    private static final String ROUTE_ID_2 = "route-2";

    @Test
    @DisplayName("should remove tenant from all module routes successfully")
    void removeTenantFromModuleRoutes_positive() {
      var route1 = new Route().id(ROUTE_ID_1).name("route-name-1")
        .expression("http.path == \"/test1\"");
      var route2 = new Route().id(ROUTE_ID_2).name("route-name-2")
        .expression("http.path == \"/test2\"");
      var routes = List.of(route1, route2);

      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null))
        .thenReturn(new KongResultList<>(null, routes));
      when(kongAdminClient.upsertRoute(anyString(), eq("route-name-1"), any(Route.class))).thenReturn(route1);
      when(kongAdminClient.upsertRoute(anyString(), eq("route-name-2"), any(Route.class))).thenReturn(route2);
      when(kongRouteTenantService.removeTenant(route1, TENANT_NAME)).thenReturn(route1);
      when(kongRouteTenantService.removeTenant(route2, TENANT_NAME)).thenReturn(route2);

      kongGatewayService.removeTenantFromModuleRoutes(MOD_ID, TENANT_NAME);

      verify(kongAdminClient).getService(MOD_ID);
      verify(kongAdminClient).getRoutesByTag(ROUTE_TAGS, null);
      verify(kongRouteTenantService).removeTenant(route1, TENANT_NAME);
      verify(kongRouteTenantService).removeTenant(route2, TENANT_NAME);
      verify(kongAdminClient, times(2)).upsertRoute(anyString(), anyString(), any(Route.class));
    }

    @Test
    @DisplayName("should handle empty routes list")
    void removeTenantFromModuleRoutes_emptyRoutes() {
      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null))
        .thenReturn(new KongResultList<>(null, emptyList()));

      kongGatewayService.removeTenantFromModuleRoutes(MOD_ID, TENANT_NAME);

      verify(kongAdminClient).getService(MOD_ID);
      verify(kongAdminClient).getRoutesByTag(ROUTE_TAGS, null);
      verify(kongRouteTenantService, never()).removeTenant(any(), any());
      verify(kongAdminClient, never()).upsertRoute(anyString(), anyString(), any(Route.class));
    }

    @Test
    @DisplayName("should throw exception when service not found")
    void removeTenantFromModuleRoutes_serviceNotFound() {
      var request = create(GET, "/services/" + MOD_ID, emptyMap(), null, (RequestTemplate) null);
      when(kongAdminClient.getService(MOD_ID))
        .thenThrow(new NotFound("Service not found", request, null, emptyMap()));

      assertThatThrownBy(() -> kongGatewayService.removeTenantFromModuleRoutes(MOD_ID, TENANT_NAME))
        .isInstanceOf(TenantRouteUpdateException.class)
        .hasMessageContaining("Failed to remove tenant")
        .hasMessageContaining(TENANT_NAME)
        .hasMessageContaining(MOD_ID);

      verify(kongAdminClient).getService(MOD_ID);
    }

    @Test
    @DisplayName("should throw exception when route update fails")
    void removeTenantFromModuleRoutes_routeUpdateFails() {
      var route1 = new Route().id(ROUTE_ID_1).name("route-name-1")
        .expression("http.path == \"/test1\"");
      var routes = List.of(route1);
      var request = create(PUT, "/services/" + SERVICE_ID + "/routes/route-name-1", emptyMap(), null, 
        (RequestTemplate) null);

      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null))
        .thenReturn(new KongResultList<>(null, routes));
      when(kongRouteTenantService.removeTenant(route1, TENANT_NAME)).thenReturn(route1);
      doThrow(new InternalServerError("Update failed", request, null, emptyMap()))
        .when(kongAdminClient).upsertRoute(anyString(), anyString(), any(Route.class));

      assertThatThrownBy(() -> kongGatewayService.removeTenantFromModuleRoutes(MOD_ID, TENANT_NAME))
        .isInstanceOf(TenantRouteUpdateException.class)
        .hasMessageContaining("Failed to remove tenant")
        .hasMessageContaining(TENANT_NAME)
        .hasMessageContaining(MOD_ID);

      verify(kongAdminClient).getService(MOD_ID);
      verify(kongAdminClient).getRoutesByTag(ROUTE_TAGS, null);
      verify(kongRouteTenantService).removeTenant(route1, TENANT_NAME);
      verify(kongAdminClient).upsertRoute(anyString(), anyString(), any(Route.class));
    }

    @Test
    @DisplayName("should throw exception when partial routes update fails")
    void removeTenantFromModuleRoutes_partialFailure() {
      var route1 = new Route().id(ROUTE_ID_1).name("route-name-1")
        .expression("http.path == \"/test1\"");
      var route2 = new Route().id(ROUTE_ID_2).name("route-name-2")
        .expression("http.path == \"/test2\"");
      var routes = List.of(route1, route2);
      var request = create(PUT, "/services/" + SERVICE_ID + "/routes/route-name-2", emptyMap(), null, 
        (RequestTemplate) null);

      when(kongAdminClient.getService(MOD_ID)).thenReturn(kongService());
      when(kongAdminClient.getRoutesByTag(ROUTE_TAGS, null))
        .thenReturn(new KongResultList<>(null, routes));
      when(kongAdminClient.upsertRoute(anyString(), eq("route-name-1"), any(Route.class))).thenReturn(route1);
      when(kongRouteTenantService.removeTenant(route1, TENANT_NAME)).thenReturn(route1);
      when(kongRouteTenantService.removeTenant(route2, TENANT_NAME)).thenReturn(route2);
      doThrow(new InternalServerError("Update failed", request, null, emptyMap()))
        .when(kongAdminClient).upsertRoute(anyString(), eq("route-name-2"), any(Route.class));

      assertThatThrownBy(() -> kongGatewayService.removeTenantFromModuleRoutes(MOD_ID, TENANT_NAME))
        .isInstanceOf(TenantRouteUpdateException.class)
        .hasMessageContaining("Failed to remove tenant")
        .hasMessageContaining("route-name-2");

      verify(kongAdminClient).getService(MOD_ID);
      verify(kongAdminClient).getRoutesByTag(ROUTE_TAGS, null);
      verify(kongRouteTenantService, times(2)).removeTenant(any(), eq(TENANT_NAME));
      verify(kongAdminClient, times(2)).upsertRoute(anyString(), anyString(), any(Route.class));
    }
  }

  static class TestValues {

    static Route route(List<String> methods, String path, String interfaceId) {
      return route(methods, path, 0, interfaceId, MOD_ID, Map.of());
    }

    static Route route(List<String> methods, String path, int priority, String interfaceId, String moduleId) {
      return route(methods, path, priority, interfaceId, moduleId, Map.of());
    }

    static Route route(List<String> methods, String path, String interfaceId, String tenant) {
      return route(methods, path, 0, interfaceId, MOD_ID, singletonMap(TENANT, tenant));
    }

    static Route route(List<String> methods, String path, int p, String interfaceId, String moduleId, String tenant) {
      return route(methods, path, p, interfaceId, moduleId, singletonMap(TENANT, tenant));
    }

    static Route route(List<String> methods, String path, int priority, String interfaceId,
      String moduleId, Map<String, String> headers) {
      var operator = path.endsWith("$") ? "~" : "==";
      var pathExpression = format("http.path %s \"%s\"", operator, path);

      var tenantHeaderExpression = "http.headers.x_okapi_tenant ~ r#\".*\"#";
      var expression = Stream.of(pathExpression, getMethodsExpression(methods), getHeadersExpression(headers),
          tenantHeaderExpression
        )
        .filter(StringUtils::isNotBlank)
        .collect(joining(" && ", "(", ")"));

      var tenantName = headers.get(TENANT);
      var routeName = Stream.of(path, join(",", methods), tenantName, moduleId, interfaceId)
        .filter(StringUtils::isNotBlank)
        .collect(joining("|"));

      var tags = Stream.of(headers.get(TENANT), moduleId, interfaceId)
        .filter(Objects::nonNull)
        .toList();

      return new Route()
        .stripPath(false)
        .priority(priority)
        .expression(expression)
        .name(sha1Hex(routeName))
        .tags(tags);
    }

    static ModuleDescriptor moduleDescriptor() {
      return new ModuleDescriptor()
        .id(MOD_ID)
        .provides(List.of(
          new InterfaceDescriptor().id("_tenant").version("1.0").interfaceType(SYSTEM_INTERFACE_TYPE).handlers(List.of(
            new RoutingEntry().methods(List.of("POST")).pathPattern("/_/tenant"),
            new RoutingEntry().methods(List.of("GET", "DELETE")).pathPattern("/_/tenant/{id}"))),
          new InterfaceDescriptor().id("test1").version("2.0").handlers(List.of(
            new RoutingEntry().methods(List.of("GET")).pathPattern("/entities/{id}"),
            new RoutingEntry().methods(List.of("PUT")).pathPattern("/entities/{id}/sub-entities"),
            new RoutingEntry().methods(List.of("*")).pathPattern("/entities/sub-entities*"),
            new RoutingEntry().methods(List.of("PUT")).path("/tests/1"),
            new RoutingEntry().methods(List.of("GET")))),
          new InterfaceDescriptor().id("test2").version("1.0").handlers(List.of(
            new RoutingEntry().methods(emptyList()).path("/test2"),
            new RoutingEntry().methods(List.of("GET")).path(""),
            new RoutingEntry().methods(List.of("GET")).pathPattern(""),
            new RoutingEntry().methods(List.of("GET")).pathPattern("/test2-entities")))
        ));
    }

    static ModuleDescriptor mdWithMultipleInterface1() {
      return new ModuleDescriptor().id("mod-foo-1.0.0").provides(List.of(
        new InterfaceDescriptor().id("baz-multiple").version("1.0").interfaceType("multiple").addHandlersItem(
          new RoutingEntry().methods(List.of("GET")).pathPattern("/baz/entities")),
        new InterfaceDescriptor().id("foo").version("1.0").addHandlersItem(
          new RoutingEntry().methods(List.of("POST")).pathPattern("/foo/entities"))
      ));
    }

    static ModuleDescriptor mdWithMultipleInterface2() {
      return new ModuleDescriptor().id("mod-bar-1.0.0").provides(List.of(
        new InterfaceDescriptor().id("baz-multiple").version("1.0").interfaceType("multiple").addHandlersItem(
          new RoutingEntry().methods(List.of("GET")).pathPattern("/baz/entities")),
        new InterfaceDescriptor().id("bar").version("1.0").addHandlersItem(
          new RoutingEntry().methods(List.of("POST")).pathPattern("/bar/entities"))
      ));
    }

    static ModuleDescriptor mdWithTimerInterface() {
      var md = moduleDescriptor();

      var provides = new ArrayList<>(md.getProvides());
      provides.add(
        new InterfaceDescriptor().id(TIMER_INTERFACE).version("1.0").interfaceType(SYSTEM_INTERFACE_TYPE).handlers(
          List.of(
            new RoutingEntry().methods(List.of("POST")).pathPattern("/test/timer1"),
            new RoutingEntry().methods(List.of("POST")).pathPattern("/test/timer2")
          ))
      );

      md.setProvides(provides);

      return md;
    }

    static Map<String, String> multipleTypeHeaders(String moduleId) {
      var headers = new LinkedHashMap<String, String>();
      headers.put(MODULE_ID, moduleId);
      return headers;
    }

    static String getHeadersExpression(Map<String, String> headers) {
      if (headers.isEmpty()) {
        return null;
      }

      if (headers.size() == 1) {
        var entry = headers.entrySet().iterator().next();
        var updatedHeaderName = entry.getKey().replaceAll("-", "_");
        return "http.headers." + updatedHeaderName + " == \"" + entry.getValue() + "\"";
      }

      return headers.entrySet().stream()
        .map(headersEntry -> getHeadersExpression(Map.ofEntries(headersEntry)))
        .collect(joining(" && ", "(", ")"));
    }

    static String getMethodsExpression(List<String> methods) {
      if (methods.size() == 1) {
        return "http.method == \"" + methods.get(0) + "\"";
      }

      return methods.stream()
        .map(method -> getMethodsExpression(singletonList(method)))
        .collect(joining(" || ", "(", ")"));
    }

    static Service kongService() {
      return new Service().id(SERVICE_ID).name(MOD_ID).url(SERVICE_URL);
    }
  }
}
