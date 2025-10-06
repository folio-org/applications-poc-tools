package org.folio.tools.kong.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.folio.tools.kong.model.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
class KongRouteTenantServiceTest {

  private KongRouteTenantService service;

  @BeforeEach
  void setUp() {
    service = new KongRouteTenantService();
  }

  @Nested
  @DisplayName("addTenant")
  class AddTenant {

    @Test
    @DisplayName("should add tenant clause when expression has no tenant information")
    void addTenant_noTenantInfo() {
      var route = new Route()
        .id("test-route-1")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\")");

      var result = service.addTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\")");
    }

    @Test
    @DisplayName("should replace wildcard tenant pattern with specific tenant")
    void addTenant_wildcardPattern() {
      var route = new Route()
        .id("test-route-2")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& http.headers.x_okapi_tenant ~ r#\".*\"#)");

      var result = service.addTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\"))");
    }

    @Test
    @DisplayName("should replace wildcard tenant pattern in bracers with specific tenant")
    void addTenant_wildcardPatternInBracers() {
      var route = new Route()
        .id("test-route-2")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant ~ r#\".*\"#))");

      var result = service.addTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\"))");
    }

    @Test
    @DisplayName("should add tenant to existing single tenant clause")
    void addTenant_singleTenantExists() {
      var route = new Route()
        .id("test-route-3")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\"))");

      var result = service.addTenant(route, "tenant2");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" || http.headers.x_okapi_tenant == \"tenant2\"))");
    }

    @Test
    @DisplayName("should add tenant to existing multiple tenant clause")
    void addTenant_multipleTenants() {
      var route = new Route()
        .id("test-route-4")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" || http.headers.x_okapi_tenant == \"tenant2\"))");

      var result = service.addTenant(route, "tenant3");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" || http.headers.x_okapi_tenant == \"tenant2\" "
          + "|| http.headers.x_okapi_tenant == \"tenant3\"))");
    }

    @Test
    @DisplayName("should not modify expression when tenant already exists")
    void addTenant_tenantAlreadyExists() {
      var route = new Route()
        .id("test-route-5")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" || http.headers.x_okapi_tenant == \"tenant2\"))");

      var result = service.addTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" || http.headers.x_okapi_tenant == \"tenant2\"))");
    }

    @Test
    @DisplayName("should handle null route")
    void addTenant_nullRoute() {
      var result = service.addTenant(null, "tenant1");

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle null tenantId")
    void addTenant_nullTenantId() {
      var route = new Route()
        .id("test-route-6")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\")");

      var result = service.addTenant(route, null);

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\")");
    }

    @Test
    @DisplayName("should handle blank tenantId")
    void addTenant_blankTenantId() {
      var route = new Route()
        .id("test-route-7")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\")");

      var result = service.addTenant(route, "  ");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\")");
    }

    @Test
    @DisplayName("should handle null expression")
    void addTenant_nullExpression() {
      var route = new Route().id("test-route-8").expression((String) null);

      var result = service.addTenant(route, "tenant1");

      assertThat(result.getExpression()).isNull();
    }

    @Test
    @DisplayName("should handle blank expression")
    void addTenant_blankExpression() {
      var route = new Route().id("test-route-9").expression("  ");

      var result = service.addTenant(route, "tenant1");

      assertThat(result.getExpression()).isEqualTo("  ");
    }

    @Test
    @DisplayName("should handle expression without parentheses")
    void addTenant_noParentheses() {
      var route = new Route()
        .id("test-route-10")
        .expression("http.path ~ \"^/users$\" && http.method == \"GET\"");

      var result = service.addTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("http.path ~ \"^/users$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\")");
    }

    @Test
    @DisplayName("should handle wildcard pattern with parentheses")
    void addTenant_wildcardWithParentheses() {
      var route = new Route()
        .id("test-route-11")
        .expression("(http.path ~ \"^/users$\" && (http.headers.x_okapi_tenant ~ r#\".*\"#))");

      var result = service.addTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && (http.headers.x_okapi_tenant == \"tenant1\"))");
    }
  }

  @Nested
  @DisplayName("removeTenant")
  class RemoveTenant {

    @Test
    @DisplayName("should replace with wildcard when removing last tenant")
    void removeTenant_singleTenant() {
      var route = new Route()
        .id("test-route-20")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\"))");

      var result = service.removeTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant ~ r#\".*\"#))");
    }

    @Test
    @DisplayName("should remove tenant from multiple tenant clause")
    void removeTenant_multipleTenants() {
      var route = new Route()
        .id("test-route-21")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" || http.headers.x_okapi_tenant == \"tenant2\" "
          + "|| http.headers.x_okapi_tenant == \"tenant3\"))");

      var result = service.removeTenant(route, "tenant2");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" || http.headers.x_okapi_tenant == \"tenant3\"))");
    }

    @Test
    @DisplayName("should reduce to single tenant when removing from two tenants")
    void removeTenant_twoToOne() {
      var route = new Route()
        .id("test-route-22")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" || http.headers.x_okapi_tenant == \"tenant2\"))");

      var result = service.removeTenant(route, "tenant2");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\"))");
    }

    @Test
    @DisplayName("should not modify expression when tenant does not exist")
    void removeTenant_tenantNotExists() {
      var route = new Route()
        .id("test-route-23")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" || http.headers.x_okapi_tenant == \"tenant2\"))");

      var result = service.removeTenant(route, "tenant3");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" || http.headers.x_okapi_tenant == \"tenant2\"))");
    }

    @Test
    @DisplayName("should not modify expression when no tenant clause exists")
    void removeTenant_noTenantClause() {
      var route = new Route()
        .id("test-route-24")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\")");

      var result = service.removeTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\")");
    }

    @Test
    @DisplayName("should not modify expression with wildcard pattern")
    void removeTenant_wildcardPattern() {
      var route = new Route()
        .id("test-route-25")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& http.headers.x_okapi_tenant ~ r#\".*\"#)");

      var result = service.removeTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& http.headers.x_okapi_tenant ~ r#\".*\"#)");
    }

    @Test
    @DisplayName("should handle null route")
    void removeTenant_nullRoute() {
      var result = service.removeTenant(null, "tenant1");

      assertThat(result).isNull();
    }

    @Test
    @DisplayName("should handle null tenantId")
    void removeTenant_nullTenantId() {
      var route = new Route()
        .id("test-route-26")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\"))");

      var result = service.removeTenant(route, null);

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\"))");
    }

    @Test
    @DisplayName("should handle blank tenantId")
    void removeTenant_blankTenantId() {
      var route = new Route()
        .id("test-route-27")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\"))");

      var result = service.removeTenant(route, "  ");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\"))");
    }

    @Test
    @DisplayName("should handle null expression")
    void removeTenant_nullExpression() {
      var route = new Route().id("test-route-28").expression((String) null);

      var result = service.removeTenant(route, "tenant1");

      assertThat(result.getExpression()).isNull();
    }

    @Test
    @DisplayName("should handle blank expression")
    void removeTenant_blankExpression() {
      var route = new Route().id("test-route-29").expression("  ");

      var result = service.removeTenant(route, "tenant1");

      assertThat(result.getExpression()).isEqualTo("  ");
    }

    @Test
    @DisplayName("should replace with wildcard when tenant clause at the beginning")
    void removeTenant_clauseAtBeginning() {
      var route = new Route()
        .id("test-route-30")
        .expression("(http.headers.x_okapi_tenant == \"tenant1\") "
          + "&& http.path ~ \"^/users$\" && http.method == \"GET\"");

      var result = service.removeTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("(http.headers.x_okapi_tenant ~ r#\".*\"#) "
          + "&& http.path ~ \"^/users$\" && http.method == \"GET\"");
    }

    @Test
    @DisplayName("should replace with wildcard when tenant clause in the middle")
    void removeTenant_clauseInMiddle() {
      var route = new Route()
        .id("test-route-31")
        .expression("http.path ~ \"^/users$\" "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\") && http.method == \"GET\"");

      var result = service.removeTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("http.path ~ \"^/users$\" "
          + "&& (http.headers.x_okapi_tenant ~ r#\".*\"#) && http.method == \"GET\"");
    }

    @Test
    @DisplayName("should remove first tenant from multiple tenants")
    void removeTenant_firstTenant() {
      var route = new Route()
        .id("test-route-32")
        .expression("(http.path ~ \"^/users$\" && (http.headers.x_okapi_tenant == \"tenant1\" "
          + "|| http.headers.x_okapi_tenant == \"tenant2\" || http.headers.x_okapi_tenant == \"tenant3\"))");

      var result = service.removeTenant(route, "tenant1");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && (http.headers.x_okapi_tenant == \"tenant2\" "
          + "|| http.headers.x_okapi_tenant == \"tenant3\"))");
    }

    @Test
    @DisplayName("should remove last tenant from multiple tenants")
    void removeTenant_lastTenant() {
      var route = new Route()
        .id("test-route-33")
        .expression("(http.path ~ \"^/users$\" && (http.headers.x_okapi_tenant == \"tenant1\" "
          + "|| http.headers.x_okapi_tenant == \"tenant2\" || http.headers.x_okapi_tenant == \"tenant3\"))");

      var result = service.removeTenant(route, "tenant3");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && (http.headers.x_okapi_tenant == \"tenant1\" "
          + "|| http.headers.x_okapi_tenant == \"tenant2\"))");
    }
  }

  @Nested
  @DisplayName("integration scenarios")
  class IntegrationScenarios {

    @Test
    @DisplayName("should add and remove tenants in sequence")
    void addAndRemoveTenants() {
      var route = new Route()
        .id("test-route-40")
        .expression("(http.path ~ \"^/users$\" && http.method == \"GET\")");

      // Add first tenant
      service.addTenant(route, "tenant1");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && http.method == \"GET\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\")");

      // Add second tenant
      service.addTenant(route, "tenant2");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && http.method == \"GET\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" "
          + "|| http.headers.x_okapi_tenant == \"tenant2\")");

      // Add third tenant
      service.addTenant(route, "tenant3");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && http.method == \"GET\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" "
          + "|| http.headers.x_okapi_tenant == \"tenant2\" "
          + "|| http.headers.x_okapi_tenant == \"tenant3\")");

      // Remove middle tenant
      service.removeTenant(route, "tenant2");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && http.method == \"GET\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" "
          + "|| http.headers.x_okapi_tenant == \"tenant3\")");

      // Remove remaining tenants
      service.removeTenant(route, "tenant1");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && http.method == \"GET\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant3\")");

      service.removeTenant(route, "tenant3");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && http.method == \"GET\") "
          + "&& (http.headers.x_okapi_tenant ~ r#\".*\"#)");
    }

    @Test
    @DisplayName("should handle complex expression with multiple conditions")
    void complexExpression() {
      var route = new Route()
        .id("test-route-41")
        .expression("(http.path ~ \"^/api/v1/users$\" && http.method == \"POST\" "
          + "&& http.host == \"example.com\")");

      service.addTenant(route, "tenant1");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/api/v1/users$\" && http.method == \"POST\" "
          + "&& http.host == \"example.com\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\")");

      service.addTenant(route, "tenant2");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/api/v1/users$\" && http.method == \"POST\" "
          + "&& http.host == \"example.com\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant1\" "
          + "|| http.headers.x_okapi_tenant == \"tenant2\")");
    }

    @Test
    @DisplayName("should handle tenant names with special characters")
    void tenantsWithSpecialNames() {
      var route = new Route()
        .id("test-route-42")
        .expression("(http.path ~ \"^/users$\")");

      service.addTenant(route, "tenant-prod-001");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant-prod-001\")");

      service.addTenant(route, "tenant_dev_002");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant-prod-001\" "
          + "|| http.headers.x_okapi_tenant == \"tenant_dev_002\")");

      service.removeTenant(route, "tenant-prod-001");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\") "
          + "&& (http.headers.x_okapi_tenant == \"tenant_dev_002\")");
    }

    @Test
    @DisplayName("should replace with wildcard when removing last tenant from expression")
    void placeholderReplacement() {
      var route = new Route()
        .id("test-route-43")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" "
          + "&& http.method == \"GET\" && (http.headers.x_okapi_tenant == \"t1\"))");

      service.removeTenant(route, "t1");

      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" "
          + "&& http.method == \"GET\" && (http.headers.x_okapi_tenant ~ r#\".*\"#))");
    }

    @Test
    @DisplayName("should replace tenant with wildcard expression as per requirement example")
    void removeTenant_requirementExample() {
      var route = new Route()
        .id("test-route-44")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"t1\"))");

      var result = service.removeTenant(route, "t1");

      assertThat(result.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant ~ r#\".*\"#))");
    }

    @Test
    @DisplayName("should replace wildcard back to specific tenant when adding")
    void addTenant_afterWildcardReplacement() {
      var route = new Route()
        .id("test-route-45")
        .expression("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"t1\"))");

      // Remove the last tenant - should create wildcard
      service.removeTenant(route, "t1");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant ~ r#\".*\"#))");

      // Add a new tenant - should replace wildcard with specific tenant
      service.addTenant(route, "t2");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users/([^/]+)/capabilities$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"t2\"))");
    }

    @Test
    @DisplayName("should handle multiple tenants removal to wildcard")
    void removeTenant_multipleToWildcard() {
      var route = new Route()
        .id("test-route-46")
        .expression("(http.path ~ \"^/users$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"t1\" || http.headers.x_okapi_tenant == \"t2\"))");

      // Remove first tenant
      service.removeTenant(route, "t1");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant == \"t2\"))");

      // Remove last tenant - should create wildcard
      service.removeTenant(route, "t2");
      assertThat(route.getExpression())
        .isEqualTo("(http.path ~ \"^/users$\" && http.method == \"GET\" "
          + "&& (http.headers.x_okapi_tenant ~ r#\".*\"#))");
    }

    @Test
    @DisplayName("should handle wildcard at different positions in expression")
    void removeTenant_wildcardAtDifferentPositions() {
      // Test with tenant clause at the end
      var route1 = new Route()
        .id("test-route-47")
        .expression("http.path ~ \"^/test$\" && (http.headers.x_okapi_tenant == \"t1\")");

      service.removeTenant(route1, "t1");
      assertThat(route1.getExpression())
        .isEqualTo("http.path ~ \"^/test$\" && (http.headers.x_okapi_tenant ~ r#\".*\"#)");

      // Test with tenant clause at the start
      var route2 = new Route()
        .id("test-route-48")
        .expression("(http.headers.x_okapi_tenant == \"t1\") && http.path ~ \"^/test$\"");

      service.removeTenant(route2, "t1");
      assertThat(route2.getExpression())
        .isEqualTo("(http.headers.x_okapi_tenant ~ r#\".*\"#) && http.path ~ \"^/test$\"");
    }
  }
}
