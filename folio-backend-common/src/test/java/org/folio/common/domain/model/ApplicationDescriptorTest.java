package org.folio.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.common.domain.model.ApplicationScope.REGULAR;

import jakarta.validation.Validation;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class ApplicationDescriptorTest {

  private final JsonMapper mapper = JsonMapper.builder().build();

  @ParameterizedTest
  @EnumSource(ApplicationScope.class)
  void scope_positive_roundTrip(ApplicationScope scope) {
    var descriptor = mapper.readValue("""
      {"name": "app-test", "version": "1.0.0", "scope": "%s"}
      """.formatted(scope), ApplicationDescriptor.class);
    var roundTrip = mapper.readValue(mapper.writeValueAsString(descriptor), ApplicationDescriptor.class);

    assertThat(roundTrip.getScope()).isEqualTo(scope);
    assertThat(mapper.valueToTree(descriptor).get("scope").asString()).isEqualTo(scope.getValue());
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(roundTrip)).isEmpty();
    }
  }

  @Test
  void scope_positive_existingDescriptorDefaultsToRegular() {
    var descriptor = mapper.readValue("""
      {"id": "app-test-1.0.0", "name": "app-test", "version": "1.0.0",
       "modules": [{"id": "mod-test-1.0.0", "name": "mod-test", "version": "1.0.0"}]}
      """, ApplicationDescriptor.class);

    assertThat(descriptor.getScope()).isEqualTo(REGULAR);
    assertThat(mapper.valueToTree(descriptor).get("scope").asString()).isEqualTo("regular");
    assertThat(descriptor.getModules()).singleElement()
      .satisfies(module -> assertThat(module.getId()).isEqualTo("mod-test-1.0.0"));
    assertThat(mapper.readValue(mapper.writeValueAsString(descriptor), ApplicationDescriptor.class))
      .isEqualTo(descriptor);
    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(descriptor)).isEmpty();
    }
  }

  @ParameterizedTest
  @EmptySource
  @ValueSource(strings = {"tenant", "PLATFORM", "Regular", " platform "})
  void scope_negative_invalidValue(String scope) {
    assertThatThrownBy(() -> mapper.readValue("{\"scope\":\"" + scope + "\"}", ApplicationDescriptor.class))
      .hasRootCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void scope_negative_explicitNull() {
    var descriptor = mapper.readValue("{\"name\":\"app-test\",\"version\":\"1.0.0\",\"scope\":null}",
      ApplicationDescriptor.class);

    try (var factory = Validation.buildDefaultValidatorFactory()) {
      assertThat(factory.getValidator().validate(descriptor)).singleElement()
        .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("scope"));
    }
  }
}
