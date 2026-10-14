package org.folio.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import java.util.List;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class ModuleDescriptorTest {

  private final JsonMapper mapper = JsonMapper.builder().build();

  @Test
  void eventDeclarations_positive_roundTrip() {
    var json = """
      {
        "id": "mod-inventory-1.0.0",
        "producesEvents": [{"event": "inventory.instance.created.v1"}],
        "consumesEvents": [
          {"event": "inventory.instance.created.v1", "optional": false},
          {"event": "mod-login-keycloak.logout.v1", "optional": true}
        ]
      }
      """;

    var descriptor = mapper.readValue(json, ModuleDescriptor.class);
    var roundTrip = mapper.readValue(mapper.writeValueAsString(descriptor), ModuleDescriptor.class);

    assertThat(roundTrip.getProducesEvents()).containsExactly(
      new ProducesEvent().event("inventory.instance.created.v1"));
    assertThat(roundTrip.getConsumesEvents()).containsExactly(
      new ConsumesEvent().event("inventory.instance.created.v1").optional(false),
      new ConsumesEvent().event("mod-login-keycloak.logout.v1").optional(true));
    assertThat(roundTrip).isEqualTo(descriptor);
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(roundTrip)).isEmpty();
    }
  }

  @Test
  void consumesEvents_positive_defaultsToRequired() {
    var descriptor = mapper.readValue("""
      {"consumesEvents": [{"event": "inventory.instance.created.v1"}]}
      """, ModuleDescriptor.class);

    assertThat(descriptor.getConsumesEvents()).singleElement()
      .extracting(ConsumesEvent::getOptional).isEqualTo(false);
  }

  @ParameterizedTest
  @ValueSource(strings = {"producesEvents", "consumesEvents"})
  void eventDeclarations_negative_missingEvent(String field) {
    var descriptor = mapper.readValue("{\"" + field + "\":[{}]}", ModuleDescriptor.class);

    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(descriptor)).singleElement()
        .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString(field + "[0].event"));
      var application = new ApplicationDescriptor("app-test", "1.0.0").moduleDescriptors(List.of(descriptor));
      assertThat(factory.getValidator().validate(application)).singleElement()
        .satisfies(violation -> assertThat(violation.getPropertyPath())
          .hasToString("moduleDescriptors[0]." + field + "[0].event"));
    }
  }

  @Test
  void consumesEvents_negative_nullOptional() {
    var descriptor = mapper.readValue("""
      {"consumesEvents": [{"event": "inventory.instance.created.v1", "optional": null}]}
      """, ModuleDescriptor.class);

    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(descriptor)).singleElement()
        .satisfies(violation -> assertThat(violation.getPropertyPath())
          .hasToString("consumesEvents[0].optional"));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"producesEvents", "consumesEvents"})
  void eventDeclarations_negative_nullElement(String field) {
    var descriptor = mapper.readValue("{\"" + field + "\":[null]}", ModuleDescriptor.class);

    try (var factory = Validation.buildDefaultValidatorFactory()) {
      var application = new ApplicationDescriptor("app-test", "1.0.0").moduleDescriptors(List.of(descriptor));
      assertThat(factory.getValidator().validate(application)).singleElement()
        .satisfies(violation -> assertThat(violation.getPropertyPath().toString())
          .startsWith("moduleDescriptors[0]." + field + "[0]"));
    }
  }

  @Test
  void read_positive_existingDescriptor() {
    var descriptor = mapper.readValue("""
      {"id": "mod-inventory-1.0.0", "name": "Inventory", "provides": [
        {"id": "inventory", "version": "1.0", "handlers": [{"methods": ["GET"], "pathPattern": "/inventory"}]}
      ]}
      """, ModuleDescriptor.class);

    assertThat(descriptor.getProducesEvents()).isEmpty();
    assertThat(descriptor.getConsumesEvents()).isEmpty();
    assertThat(mapper.valueToTree(descriptor).get("producesEvents").isEmpty()).isTrue();
    assertThat(mapper.valueToTree(descriptor).get("consumesEvents").isEmpty()).isTrue();
    assertThat(descriptor.getName()).isEqualTo("mod-inventory");
    assertThat(descriptor.getVersion()).isEqualTo("1.0.0");
    assertThat(descriptor.getDescription()).isEqualTo("Inventory");
    assertThat(descriptor.getProvides()).singleElement()
      .satisfies(value -> assertThat(value.getId()).isEqualTo("inventory"));
    assertThat(mapper.readValue(mapper.writeValueAsString(descriptor), ModuleDescriptor.class)).isEqualTo(descriptor);
  }
}
