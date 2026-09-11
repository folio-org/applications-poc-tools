package org.folio.tools.apisix.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.folio.tools.apisix.model.ApisixRoute;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class ApisixRouteTenantServiceTest {

  private static final String TENANT_VAR = "http_x_okapi_tenant";
  private static final List<Object> URI_VAR = List.of("uri", "~~", "^/entities/([^/]+)$");
  private static final List<Object> WILDCARD_TENANT_VAR = List.of(TENANT_VAR, "~~", ".*");

  private final ApisixRouteTenantService service = new ApisixRouteTenantService();

  private static ApisixRoute routeWithVars(List<Object>... vars) {
    var varsList = new ArrayList<List<Object>>();
    for (var v : vars) {
      varsList.add(new ArrayList<>(v));
    }
    return new ApisixRoute().id("route-id").vars(varsList);
  }

  private static List<Object> tenantInVar(String... tenants) {
    return List.of(TENANT_VAR, "in", List.of(tenants));
  }

  @Nested
  @DisplayName("addTenant")
  class AddTenant {

    @Test
    void addTenant_positive_replacesWildcardWithTenantList() {
      var route = routeWithVars(URI_VAR, WILDCARD_TENANT_VAR);

      var changed = service.addTenant(route, "tenant1");

      assertThat(changed).isTrue();
      assertThat(route.getVars()).containsExactly(URI_VAR, tenantInVar("tenant1"));
    }

    @Test
    void addTenant_positive_appendsTenantToExistingList() {
      var route = routeWithVars(URI_VAR, tenantInVar("tenant1"));

      var changed = service.addTenant(route, "tenant2");

      assertThat(changed).isTrue();
      assertThat(route.getVars()).containsExactly(URI_VAR, tenantInVar("tenant1", "tenant2"));
    }

    @Test
    void addTenant_positive_noopWhenTenantAlreadyPresent() {
      var route = routeWithVars(URI_VAR, tenantInVar("tenant1"));

      var changed = service.addTenant(route, "tenant1");

      assertThat(changed).isFalse();
      assertThat(route.getVars()).containsExactly(URI_VAR, tenantInVar("tenant1"));
    }

    @Test
    void addTenant_positive_appendsTenantVarWhenNoTenantVarExists() {
      var route = routeWithVars(URI_VAR);

      var changed = service.addTenant(route, "tenant1");

      assertThat(changed).isTrue();
      assertThat(route.getVars()).containsExactly(URI_VAR, tenantInVar("tenant1"));
    }

    @Test
    void addTenant_negative_nullRoute() {
      assertThat(service.addTenant(null, "tenant1")).isFalse();
    }

    @Test
    void addTenant_negative_blankTenant() {
      var route = routeWithVars(URI_VAR, WILDCARD_TENANT_VAR);

      var changed = service.addTenant(route, " ");

      assertThat(changed).isFalse();
      assertThat(route.getVars()).containsExactly(URI_VAR, WILDCARD_TENANT_VAR);
    }

    @Test
    void addTenant_negative_nullVars() {
      var route = new ApisixRoute().id("route-id");

      var changed = service.addTenant(route, "tenant1");

      assertThat(changed).isFalse();
      assertThat(route.getVars()).isNull();
    }
  }

  @Nested
  @DisplayName("removeTenant")
  class RemoveTenant {

    @Test
    void removeTenant_positive_removesTenantFromList() {
      var route = routeWithVars(URI_VAR, tenantInVar("tenant1", "tenant2"));

      var changed = service.removeTenant(route, "tenant1");

      assertThat(changed).isTrue();
      assertThat(route.getVars()).containsExactly(URI_VAR, tenantInVar("tenant2"));
    }

    @Test
    void removeTenant_positive_restoresWildcardWhenLastTenantRemoved() {
      var route = routeWithVars(URI_VAR, tenantInVar("tenant1"));

      var changed = service.removeTenant(route, "tenant1");

      assertThat(changed).isTrue();
      assertThat(route.getVars()).containsExactly(URI_VAR, WILDCARD_TENANT_VAR);
    }

    @Test
    void removeTenant_positive_noopWhenTenantNotPresent() {
      var route = routeWithVars(URI_VAR, tenantInVar("tenant1"));

      var changed = service.removeTenant(route, "tenant2");

      assertThat(changed).isFalse();
      assertThat(route.getVars()).containsExactly(URI_VAR, tenantInVar("tenant1"));
    }

    @Test
    void removeTenant_positive_noopOnWildcard() {
      var route = routeWithVars(URI_VAR, WILDCARD_TENANT_VAR);

      var changed = service.removeTenant(route, "tenant1");

      assertThat(changed).isFalse();
      assertThat(route.getVars()).containsExactly(URI_VAR, WILDCARD_TENANT_VAR);
    }

    @Test
    void removeTenant_positive_noopWhenNoTenantVar() {
      var route = routeWithVars(URI_VAR);

      var changed = service.removeTenant(route, "tenant1");

      assertThat(changed).isFalse();
      assertThat(route.getVars()).containsExactly(URI_VAR);
    }

    @Test
    void removeTenant_negative_nullRoute() {
      assertThat(service.removeTenant(null, "tenant1")).isFalse();
    }

    @Test
    void removeTenant_negative_nullVars() {
      var route = new ApisixRoute().id("route-id");

      var changed = service.removeTenant(route, "tenant1");

      assertThat(changed).isFalse();
      assertThat(route.getVars()).isNull();
    }
  }
}
