package org.folio.tools.apisix.service;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.domain.model.InterfaceDescriptor.SYSTEM_INTERFACE_TYPE;

import java.util.List;
import java.util.Map;
import org.folio.common.domain.model.InterfaceDescriptor;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.test.types.UnitTest;
import org.folio.tools.apisix.model.ApisixRoute;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

@UnitTest
class ApisixRouteFactoryTest {

  private static final String MOD_ID = "mod-foo-1.0.0";
  private static final String SERVICE_ID = MOD_ID;
  private static final String INTERFACE_ID = "test1-2.0";
  private static final List<Object> WILDCARD_TENANT_VAR = List.of("http_x_okapi_tenant", "~~", ".*");

  private final ApisixRouteFactory factory = new ApisixRouteFactory();

  private static ModuleDescriptor descriptorWith(RoutingEntry... handlers) {
    return new ModuleDescriptor().id(MOD_ID).provides(List.of(
      new InterfaceDescriptor().id("test1").version("2.0").handlers(List.of(handlers))));
  }

  @Test
  void createRoutes_positive_staticPath() {
    var descriptor = descriptorWith(new RoutingEntry().methods(List.of("GET")).path("/tests/1"));

    var routes = factory.createRoutes(descriptor, SERVICE_ID);

    var expectedId = sha1Hex("/tests/1|GET|" + MOD_ID + "|" + INTERFACE_ID);
    assertThat(routes).containsExactly(new ApisixRoute()
      .id(expectedId).name(expectedId)
      .uri("/tests/1").priority(1).status(1)
      .methods(List.of("GET"))
      .serviceId(SERVICE_ID)
      .labels(Map.of("module", MOD_ID, "interface", INTERFACE_ID))
      .vars(List.of(WILDCARD_TENANT_VAR)));
  }

  @Test
  void createRoutes_positive_pathVariablePattern() {
    var descriptor = descriptorWith(new RoutingEntry().methods(List.of("GET")).pathPattern("/entities/{id}"));

    var routes = factory.createRoutes(descriptor, SERVICE_ID);

    var expectedRegex = "^/entities/([^/]+)$";
    var expectedId = sha1Hex(expectedRegex + "|GET|" + MOD_ID + "|" + INTERFACE_ID);
    assertThat(routes).containsExactly(new ApisixRoute()
      .id(expectedId).name(expectedId)
      .uri("/entities/*").priority(0).status(1)
      .methods(List.of("GET"))
      .serviceId(SERVICE_ID)
      .labels(Map.of("module", MOD_ID, "interface", INTERFACE_ID))
      .vars(List.of(List.of("uri", "~~", expectedRegex), WILDCARD_TENANT_VAR)));
  }

  @Test
  void createRoutes_positive_asteriskPatternWithAllMethods() {
    var descriptor = descriptorWith(new RoutingEntry().methods(List.of("*")).pathPattern("/entities/sub-entities*"));

    var routes = factory.createRoutes(descriptor, SERVICE_ID);

    var allMethods = stream(HttpMethod.values()).map(HttpMethod::name).toList();
    assertThat(routes).hasSize(1);
    var route = routes.get(0);
    assertThat(route.getUri()).isEqualTo("/entities/sub-entities*");
    assertThat(route.getPriority()).isZero();
    assertThat(route.getMethods()).isEqualTo(allMethods);
    assertThat(route.getVars())
      .containsExactly(List.of("uri", "~~", "^/entities/sub-entities(.*)$"), WILDCARD_TENANT_VAR);
  }

  @Test
  void createRoutes_positive_multipleInterfaceAddsModuleIdVar() {
    var descriptor = new ModuleDescriptor().id(MOD_ID).provides(List.of(
      new InterfaceDescriptor().id("baz-multiple").version("1.0").interfaceType("multiple").addHandlersItem(
        new RoutingEntry().methods(List.of("GET")).pathPattern("/baz/entities"))));

    var routes = factory.createRoutes(descriptor, SERVICE_ID);

    assertThat(routes).hasSize(1);
    assertThat(routes.get(0).getVars()).containsExactly(
      List.of("http_x_okapi_module_id", "==", MOD_ID),
      WILDCARD_TENANT_VAR);
  }

  @Test
  void createRoutes_positive_mgrComponentHasNoTenantVar() {
    var descriptor = new ModuleDescriptor().id("mgr-foo-1.0.0").provides(List.of(
      new InterfaceDescriptor().id("test1").version("2.0").handlers(List.of(
        new RoutingEntry().methods(List.of("GET")).path("/tests/1")))));

    var routes = factory.createRoutes(descriptor, "mgr-foo-1.0.0");

    assertThat(routes).hasSize(1);
    assertThat(routes.get(0).getVars()).isNull();
  }

  @Test
  void createRoutes_positive_systemInterfaceSkipped() {
    var descriptor = new ModuleDescriptor().id(MOD_ID).provides(List.of(
      new InterfaceDescriptor().id("_tenant").version("1.0").interfaceType(SYSTEM_INTERFACE_TYPE).handlers(List.of(
        new RoutingEntry().methods(List.of("POST")).pathPattern("/_/tenant")))));

    var routes = factory.createRoutes(descriptor, SERVICE_ID);

    assertThat(routes).isEmpty();
  }

  @Test
  void createRoutes_positive_invalidHandlersSkipped() {
    var descriptor = descriptorWith(
      new RoutingEntry().methods(emptyList()).path("/test2"),
      new RoutingEntry().methods(List.of("GET")).path(""),
      new RoutingEntry().methods(List.of("GET")).pathPattern(""),
      new RoutingEntry().methods(List.of("GET")));

    var routes = factory.createRoutes(descriptor, SERVICE_ID);

    assertThat(routes).isEmpty();
  }

  @Test
  void createRoutes_positive_unbalancedClosingBrace() {
    var descriptor = descriptorWith(new RoutingEntry().methods(List.of("GET")).pathPattern("/tests/ids}"));

    var routes = factory.createRoutes(descriptor, SERVICE_ID);

    assertThat(routes).hasSize(1);
    assertThat(routes.get(0).getUri()).isEqualTo("/tests/ids*");
    assertThat(routes.get(0).getPriority()).isZero();
  }

  @Test
  void createRoutes_positive_deterministicIds() {
    var descriptor = descriptorWith(new RoutingEntry().methods(List.of("GET")).pathPattern("/entities/{id}"));

    var first = factory.createRoutes(descriptor, SERVICE_ID);
    var second = factory.createRoutes(descriptor, SERVICE_ID);

    assertThat(first).isEqualTo(second);
  }
}
