package org.folio.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class RoutingEntryTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testCapabilityAndModuleCapabilities() {
    var capability = new Capability()
      .name("foo_item.create")
      .resource("Foo Item")
      .action("create")
      .type("data");

    var routingEntry = new RoutingEntry()
      .methods(List.of("POST"))
      .pathPattern("/foo")
      .capability(capability)
      .moduleCapabilities(new ArrayList<>(List.of("mod_cap.view")))
      .addModuleCapabilitiesItem("mod_cap.edit");

    assertThat(routingEntry.getCapability()).isEqualTo(capability);
    assertThat(routingEntry.getModuleCapabilities()).containsExactly("mod_cap.view", "mod_cap.edit");
    assertThat(routingEntry.hasCapabilities()).isTrue();
    assertThat(routingEntry.hasPermissions()).isFalse();
  }

  @Test
  void testHasPermissions_variousSources() {
    assertThat(new RoutingEntry().hasPermissions()).isFalse();

    assertThat(new RoutingEntry()
      .permissionsRequired(List.of("perm.get"))
      .hasPermissions()).isTrue();

    assertThat(new RoutingEntry()
      .permissionsDesired(List.of("perm.get"))
      .hasPermissions()).isTrue();

    assertThat(new RoutingEntry()
      .modulePermissions(List.of("perm.get"))
      .hasPermissions()).isTrue();

    assertThat(new RoutingEntry()
      .permissionsRequiredTenant(List.of("perm.get"))
      .hasPermissions()).isTrue();
  }

  @Test
  void testHasCapabilities_variousSources() {
    assertThat(new RoutingEntry().hasCapabilities()).isFalse();

    assertThat(new RoutingEntry()
      .capability(new Capability().name("foo.view"))
      .hasCapabilities()).isTrue();

    assertThat(new RoutingEntry()
      .moduleCapabilities(List.of("mod_cap.view"))
      .hasCapabilities()).isTrue();
  }

  @Test
  void testJsonDeserialization_withCapability() throws Exception {
    var json = """
      {
        "methods": [ "GET" ],
        "pathPattern": "/roles/{id}",
        "permissionsRequired": [ "roles.item.get" ],
        "capability": {
          "permissionName": "roles.item.get",
          "name": "foo_item.create",
          "description": "Sample: Create foo item",
          "resource": "Foo Item",
          "action": "create",
          "type": "data"
        },
        "moduleCapabilities": [ "mod_cap.read" ]
      }
      """;

    var routingEntry = objectMapper.readValue(json, RoutingEntry.class);
    assertThat(routingEntry.getMethods()).containsExactly("GET");
    assertThat(routingEntry.getPathPattern()).isEqualTo("/roles/{id}");
    assertThat(routingEntry.getPermissionsRequired()).containsExactly("roles.item.get");
    assertThat(routingEntry.getModuleCapabilities()).containsExactly("mod_cap.read");

    var cap = routingEntry.getCapability();
    assertThat(cap).isNotNull();
    assertThat(cap.getName()).isEqualTo("foo_item.create");
    assertThat(cap.getDescription()).isEqualTo("Sample: Create foo item");
    assertThat(cap.getResource()).isEqualTo("Foo Item");
    assertThat(cap.getAction()).isEqualTo("create");
    assertThat(cap.getType()).isEqualTo("data");
    assertThat(cap.getPermissionName()).isEqualTo("roles.item.get");

    assertThat(routingEntry.hasPermissions()).isTrue();
    assertThat(routingEntry.hasCapabilities()).isTrue();
  }
}
