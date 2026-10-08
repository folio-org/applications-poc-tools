package org.folio.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CapabilityTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testFluentGettersAndSetters() {
    var capability = new Capability()
      .id("cap-id-1")
      .name("foo_item.create")
      .description("Sample: Create foo item")
      .resource("Foo Item")
      .action("create")
      .type("data")
      .permissionName("roles.item.get")
      .replaces(new ArrayList<>(List.of("old.perm")))
      .addReplacesItem("another.old.perm")
      .visible(true)
      .applicationId("app-1.0.0")
      .permissions(new ArrayList<>(List.of("foo.create")))
      .addPermissionsItem("foo.add")
      .capabilities(Map.of("key", List.of("val")));

    assertThat(capability.getId()).isEqualTo("cap-id-1");
    assertThat(capability.getName()).isEqualTo("foo_item.create");
    assertThat(capability.getDescription()).isEqualTo("Sample: Create foo item");
    assertThat(capability.getResource()).isEqualTo("Foo Item");
    assertThat(capability.getAction()).isEqualTo("create");
    assertThat(capability.getType()).isEqualTo("data");
    assertThat(capability.getPermissionName()).isEqualTo("roles.item.get");
    assertThat(capability.getPermission()).isEqualTo("roles.item.get");
    assertThat(capability.getReplaces()).containsExactly("old.perm", "another.old.perm");
    assertThat(capability.getVisible()).isTrue();
    assertThat(capability.getApplicationId()).isEqualTo("app-1.0.0");
    assertThat(capability.getPermissions()).containsExactly("foo.create", "foo.add");
    assertThat(capability.getCapabilities()).containsEntry("key", List.of("val"));
  }

  @Test
  void testPermissionAliasSetter() {
    var capability = new Capability().permission("roles.item.get");
    assertThat(capability.getPermissionName()).isEqualTo("roles.item.get");
  }

  @Test
  void testJsonDeserialization_withPermissionName() throws Exception {
    var json = """
      {
        "name": "foo_item.create",
        "description": "Sample: Create foo item",
        "resource": "Foo Item",
        "action": "create",
        "type": "data",
        "permissionName": "roles.item.get"
      }
      """;
    var capability = objectMapper.readValue(json, Capability.class);
    assertThat(capability.getName()).isEqualTo("foo_item.create");
    assertThat(capability.getDescription()).isEqualTo("Sample: Create foo item");
    assertThat(capability.getResource()).isEqualTo("Foo Item");
    assertThat(capability.getAction()).isEqualTo("create");
    assertThat(capability.getType()).isEqualTo("data");
    assertThat(capability.getPermissionName()).isEqualTo("roles.item.get");
  }

  @Test
  void testJsonDeserialization_withPermissionAlias() throws Exception {
    var json = """
      {
        "name": "foo_item.get",
        "resource": "Foo Item",
        "action": "view",
        "type": "data",
        "permission": "roles.item.get"
      }
      """;
    var capability = objectMapper.readValue(json, Capability.class);
    assertThat(capability.getName()).isEqualTo("foo_item.get");
    assertThat(capability.getPermissionName()).isEqualTo("roles.item.get");
  }

  @Test
  void testJsonRoundTrip() throws Exception {
    var original = new Capability()
      .name("foo_item.create")
      .description("Sample: Create foo item")
      .resource("Foo Item")
      .action("create")
      .type("data")
      .permissionName("roles.item.get");

    var json = objectMapper.writeValueAsString(original);
    assertThat(json).contains("\"permissionName\":\"roles.item.get\"");
    assertThat(json).doesNotContain("\"permission\":");

    var deserialized = objectMapper.readValue(json, Capability.class);

    assertThat(deserialized).isEqualTo(original);
  }
}
