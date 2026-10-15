package org.folio.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CapabilitySetTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testFluentGettersAndSetters() {
    var capabilitySet = new CapabilitySet()
      .id("capset-id-1")
      .name("foo_items_set.create")
      .description("Sample: Create foo item")
      .resource("Foo Items Set")
      .action("create")
      .type("data")
      .permission("foo.items.set")
      .capabilities(new ArrayList<>(List.of("foo_item.create")))
      .addCapabilitiesItem("foo_item.add")
      .replaces(new ArrayList<>(List.of("old.set")))
      .addReplacesItem("another.old.set")
      .visible(true)
      .applicationId("app-1.0.0")
      .moduleId("mod-foo-1.0.0");

    assertThat(capabilitySet.getId()).isEqualTo("capset-id-1");
    assertThat(capabilitySet.getName()).isEqualTo("foo_items_set.create");
    assertThat(capabilitySet.getDescription()).isEqualTo("Sample: Create foo item");
    assertThat(capabilitySet.getResource()).isEqualTo("Foo Items Set");
    assertThat(capabilitySet.getAction()).isEqualTo("create");
    assertThat(capabilitySet.getType()).isEqualTo("data");
    assertThat(capabilitySet.getPermission()).isEqualTo("foo.items.set");
    assertThat(capabilitySet.getPermissions()).isEqualTo("foo.items.set");
    assertThat(capabilitySet.getPermissionName()).isEqualTo("foo.items.set");
    assertThat(capabilitySet.getCapabilities()).containsExactly("foo_item.create", "foo_item.add");
    assertThat(capabilitySet.getReplaces()).containsExactly("old.set", "another.old.set");
    assertThat(capabilitySet.getVisible()).isTrue();
    assertThat(capabilitySet.getApplicationId()).isEqualTo("app-1.0.0");
    assertThat(capabilitySet.getModuleId()).isEqualTo("mod-foo-1.0.0");
  }

  @Test
  void testPermissionsAliasSetter() {
    var capabilitySet = new CapabilitySet().permissions("foo.items.set");
    assertThat(capabilitySet.getPermission()).isEqualTo("foo.items.set");
    assertThat(capabilitySet.getPermissions()).isEqualTo("foo.items.set");
    assertThat(capabilitySet.getPermissionName()).isEqualTo("foo.items.set");

    capabilitySet.permissionName("foo.items.alt");
    assertThat(capabilitySet.getPermission()).isEqualTo("foo.items.alt");
  }

  @Test
  void testJsonDeserialization_withPermissionsField() throws Exception {
    var json = """
      {
        "name": "foo_items_set.create",
        "description": "Sample: Create foo item",
        "resource": "Foo Items Set",
        "action": "create",
        "type": "data",
        "permissions": "foo.items.set",
        "capabilities": [
          "foo_item.create"
        ]
      }
      """;
    var capabilitySet = objectMapper.readValue(json, CapabilitySet.class);
    assertThat(capabilitySet.getName()).isEqualTo("foo_items_set.create");
    assertThat(capabilitySet.getDescription()).isEqualTo("Sample: Create foo item");
    assertThat(capabilitySet.getResource()).isEqualTo("Foo Items Set");
    assertThat(capabilitySet.getAction()).isEqualTo("create");
    assertThat(capabilitySet.getType()).isEqualTo("data");
    assertThat(capabilitySet.getPermission()).isEqualTo("foo.items.set");
    assertThat(capabilitySet.getPermissions()).isEqualTo("foo.items.set");
    assertThat(capabilitySet.getCapabilities()).containsExactly("foo_item.create");
  }

  @Test
  void testJsonDeserialization_withPermissionField() throws Exception {
    var json = """
      {
        "name": "foo_items_set.view",
        "resource": "Foo Items Set",
        "action": "view",
        "type": "data",
        "permission": "foo.items.get",
        "capabilities": [
          "foo_item.view"
        ]
      }
      """;
    var capabilitySet = objectMapper.readValue(json, CapabilitySet.class);
    assertThat(capabilitySet.getPermission()).isEqualTo("foo.items.get");
  }

  @Test
  void testJsonRoundTrip() throws Exception {
    var original = new CapabilitySet()
      .name("foo_items_set.create")
      .description("Sample: Create foo item")
      .resource("Foo Items Set")
      .action("create")
      .type("data")
      .permission("foo.items.set")
      .capabilities(List.of("foo_item.create"));

    var json = objectMapper.writeValueAsString(original);
    assertThat(json).contains("\"permission\":\"foo.items.set\"");
    assertThat(json).doesNotContain("\"permissions\":");
    assertThat(json).doesNotContain("\"permissionName\":");

    var deserialized = objectMapper.readValue(json, CapabilitySet.class);

    assertThat(deserialized).isEqualTo(original);
  }
}
