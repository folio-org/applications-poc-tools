package org.folio.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ModuleDescriptorTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void ac1_capabilitiesDeclaredInModuleDescriptorAreValid() throws Exception {
    var json = """
      {
        "id": "mod-foo-1.0.0",
        "name": "mod-foo",
        "capabilities": [
          {
            "name": "foo_item.create",
            "description": "Create foo item",
            "resource": "Foo Item",
            "action": "create",
            "type": "data"
          },
          {
            "name": "foo_item.view",
            "description": "View foo item",
            "resource": "Foo Item",
            "action": "view",
            "type": "data"
          }
        ]
      }
      """;

    var md = objectMapper.readValue(json, ModuleDescriptor.class);

    assertThat(md.getId()).isEqualTo("mod-foo-1.0.0");
    assertThat(md.getDescription()).isEqualTo("mod-foo");
    assertThat(md.getCapabilities()).hasSize(2);
    assertThat(md.getCapabilities().get(0).getName()).isEqualTo("foo_item.create");
    assertThat(md.getCapabilities().get(1).getName()).isEqualTo("foo_item.view");
    assertThat(md.hasCapabilities()).isTrue();
    assertThat(md.hasPermissions()).isFalse();
    assertThat(md.hasMixedPermissionsAndCapabilities()).isFalse();
  }

  @Test
  void ac2_capabilitySetsDeclaredInModuleDescriptorAreValid() throws Exception {
    var json = """
      {
        "id": "mod-foo-1.0.0",
        "name": "mod-foo",
        "capabilitySets": [
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
        ]
      }
      """;

    var md = objectMapper.readValue(json, ModuleDescriptor.class);

    assertThat(md.getId()).isEqualTo("mod-foo-1.0.0");
    assertThat(md.getCapabilitySets()).hasSize(1);
    var capSet = md.getCapabilitySets().get(0);
    assertThat(capSet.getName()).isEqualTo("foo_items_set.create");
    assertThat(capSet.getDescription()).isEqualTo("Sample: Create foo item");
    assertThat(capSet.getResource()).isEqualTo("Foo Items Set");
    assertThat(capSet.getAction()).isEqualTo("create");
    assertThat(capSet.getType()).isEqualTo("data");
    assertThat(capSet.getPermission()).isEqualTo("foo.items.set");
    assertThat(capSet.getCapabilities()).containsExactly("foo_item.create");
    assertThat(md.hasCapabilities()).isTrue();
    assertThat(md.hasPermissions()).isFalse();
    assertThat(md.hasMixedPermissionsAndCapabilities()).isFalse();
  }

  @Test
  void ac3_backwardsCompatibility_permissionsOnlyDescriptorStillWorks() throws Exception {
    var json = """
      {
        "id": "mod-foo-1.0.0",
        "name": "mod-foo",
        "provides": [
          {
            "id": "foo",
            "version": "1.0",
            "handlers": [
              {
                "methods": [ "GET" ],
                "pathPattern": "/foo/{id}",
                "permissionsRequired": [ "foo.item.get" ]
              }
            ]
          }
        ],
        "permissionSets": [
          {
            "permissionName": "foo.item.get",
            "displayName": "Foo - get an item",
            "description": "Get a Foo item"
          }
        ]
      }
      """;

    var md = objectMapper.readValue(json, ModuleDescriptor.class);

    assertThat(md.getId()).isEqualTo("mod-foo-1.0.0");
    assertThat(md.getPermissionSets()).hasSize(1);
    assertThat(md.getCapabilities()).isEmpty();
    assertThat(md.getCapabilitySets()).isEmpty();
    assertThat(md.hasPermissions()).isTrue();
    assertThat(md.hasCapabilities()).isFalse();
    assertThat(md.hasMixedPermissionsAndCapabilities()).isFalse();
  }

  @Test
  void ac4_mixedDescriptor_deserializesCleanlyAndDetectsMixedState() throws Exception {
    var json = """
      {
        "id": "mod-foo-1.0.0",
        "name": "mod-foo",
        "provides": [
          {
            "id": "foo",
            "version": "1.0",
            "handlers": [
              {
                "methods": [ "GET" ],
                "pathPattern": "/foo/{id}",
                "permissionsRequired": [ "foo.item.get" ]
              }
            ]
          }
        ],
        "permissionSets": [
          {
            "permissionName": "foo.item.get",
            "displayName": "Foo - get an item"
          }
        ],
        "capabilities": [
          {
            "name": "foo_item.create",
            "resource": "Foo Item",
            "action": "create",
            "type": "data"
          }
        ]
      }
      """;

    var md = objectMapper.readValue(json, ModuleDescriptor.class);

    assertThat(md.getId()).isEqualTo("mod-foo-1.0.0");
    assertThat(md.getPermissionSets()).hasSize(1);
    assertThat(md.getCapabilities()).hasSize(1);
    assertThat(md.hasPermissions()).isTrue();
    assertThat(md.hasCapabilities()).isTrue();
    assertThat(md.hasMixedPermissionsAndCapabilities()).isTrue();
  }

  @Test
  void requirement1_1_interfaceHandlerWithCapability() throws Exception {
    var json = """
      {
        "id": "mod-roles-1.1.0",
        "name": "mod-roles",
        "provides": [
          {
            "id": "roles",
            "version": "1.1",
            "handlers": [
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
                }
              }
            ]
          }
        ]
      }
      """;

    var md = objectMapper.readValue(json, ModuleDescriptor.class);

    assertThat(md.getProvides()).hasSize(1);
    var iface = md.getProvides().get(0);
    assertThat(iface.getId()).isEqualTo("roles");
    assertThat(iface.getVersion()).isEqualTo("1.1");
    assertThat(iface.getHandlers()).hasSize(1);

    var handler = iface.getHandlers().get(0);
    assertThat(handler.getMethods()).containsExactly("GET");
    assertThat(handler.getPathPattern()).isEqualTo("/roles/{id}");
    assertThat(handler.getPermissionsRequired()).containsExactly("roles.item.get");

    var cap = handler.getCapability();
    assertThat(cap).isNotNull();
    assertThat(cap.getName()).isEqualTo("foo_item.create");
    assertThat(cap.getDescription()).isEqualTo("Sample: Create foo item");
    assertThat(cap.getResource()).isEqualTo("Foo Item");
    assertThat(cap.getAction()).isEqualTo("create");
    assertThat(cap.getType()).isEqualTo("data");
    assertThat(cap.getPermissionName()).isEqualTo("roles.item.get");

    assertThat(iface.hasPermissions()).isTrue();
    assertThat(iface.hasCapabilities()).isTrue();
    assertThat(md.hasMixedPermissionsAndCapabilities()).isTrue();
  }

  @Test
  void testFluentMethods_moduleDescriptor() {
    var cap = new Capability().name("foo.item.get");
    var capSet = new CapabilitySet().name("foo.items.all");

    var md = new ModuleDescriptor()
      .id("mod-foo-1.0.0")
      .capabilities(new ArrayList<>(List.of(cap)))
      .addCapabilitiesItem(new Capability().name("foo.item.post"))
      .capabilitySets(new ArrayList<>(List.of(capSet)))
      .addCapabilitySetsItem(new CapabilitySet().name("foo.items.manage"));

    assertThat(md.getCapabilities()).hasSize(2);
    assertThat(md.getCapabilitySets()).hasSize(2);
    assertThat(md.hasCapabilities()).isTrue();
    assertThat(md.hasPermissions()).isFalse();
  }

  @Test
  void testFluentMethods_interfaceDescriptor() {
    var cap = new Capability().name("roles.view");
    var capSet = new CapabilitySet().name("roles.all");

    var iface = new InterfaceDescriptor("roles", "1.0")
      .capabilities(new ArrayList<>(List.of(cap)))
      .addCapabilitiesItem(new Capability().name("roles.create"))
      .capabilitySets(new ArrayList<>(List.of(capSet)))
      .addCapabilitySetsItem(new CapabilitySet().name("roles.manage"));

    assertThat(iface.getCapabilities()).hasSize(2);
    assertThat(iface.getCapabilitySets()).hasSize(2);
    assertThat(iface.hasCapabilities()).isTrue();
    assertThat(iface.hasPermissions()).isFalse();
  }

  @Test
  void testEmptyDescriptor_hasNoPermissionsOrCapabilities() {
    var md = new ModuleDescriptor().id("mod-empty-1.0.0");
    assertThat(md.hasPermissions()).isFalse();
    assertThat(md.hasCapabilities()).isFalse();
    assertThat(md.hasMixedPermissionsAndCapabilities()).isFalse();
  }

  @Test
  void testNullElementsRobustness() {
    final var md = new ModuleDescriptor().id("mod-nulls-1.0.0");
    var provides = new ArrayList<InterfaceDescriptor>();
    provides.add(null);
    var ifaceWithNullHandler = new InterfaceDescriptor("test", "1.0");
    var handlers = new ArrayList<RoutingEntry>();
    handlers.add(null);
    ifaceWithNullHandler.setHandlers(handlers);
    provides.add(ifaceWithNullHandler);
    md.setProvides(provides);

    var filters = new ArrayList<RoutingEntry>();
    filters.add(null);
    md.setFilters(filters);

    assertThat(md.hasPermissions()).isFalse();
    assertThat(md.hasCapabilities()).isFalse();
    assertThat(md.hasMixedPermissionsAndCapabilities()).isFalse();
  }

  @Test
  void testRoundTripSerialization() throws Exception {
    var cap = new Capability()
      .name("foo_item.create")
      .description("Sample: Create foo item")
      .resource("Foo Item")
      .action("create")
      .type("data")
      .permissionName("roles.item.get");

    var capSet = new CapabilitySet()
      .name("foo_items_set.create")
      .description("Sample: Create foo item")
      .resource("Foo Items Set")
      .action("create")
      .type("data")
      .permission("foo.items.set")
      .capabilities(List.of("foo_item.create"));

    var handler = new RoutingEntry()
      .methods(List.of("POST"))
      .pathPattern("/foo")
      .capability(cap);

    var iface = new InterfaceDescriptor("foo", "1.0")
      .handlers(List.of(handler));

    var original = new ModuleDescriptor()
      .id("mod-foo-1.0.0")
      .description("mod-foo")
      .provides(List.of(iface))
      .capabilities(List.of(cap))
      .capabilitySets(List.of(capSet));

    var json = objectMapper.writeValueAsString(original);
    var deserialized = objectMapper.readValue(json, ModuleDescriptor.class);

    assertThat(deserialized.getId()).isEqualTo(original.getId());
    assertThat(deserialized.getDescription()).isEqualTo(original.getDescription());
    assertThat(deserialized.getCapabilities()).isEqualTo(original.getCapabilities());
    assertThat(deserialized.getCapabilitySets()).isEqualTo(original.getCapabilitySets());
    assertThat(deserialized.getProvides().get(0).getHandlers().get(0).getCapability())
      .isEqualTo(cap);
  }
}
