package org.folio.common.configuration.properties;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.configuration.properties.FolioEnvironment.getFolioEnvName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class FolioEnvironmentTest {

  @AfterEach
  void resetEnvPropertyValue() {
    clearProperty("env");
  }

  @Test
  void shouldReturnFolioEnvFromProperties() {
    setEnvProperty("test-env");
    assertThat(getFolioEnvName()).isEqualTo("test-env");
  }

  @Test
  void shouldReturnDefaultFolioEnvIfPropertyNotSet() {
    assertThat(getFolioEnvName()).isEqualTo("folio");
  }

  @Test
  void shouldReturnDefaultFolioEnvIfPropertyIsEmpty() {
    setEnvProperty("   ");
    assertThat(getFolioEnvName()).isEqualTo("folio");
  }

  @ValueSource(strings = {"a", "Z", "0", "9", "_", "-"})
  @ParameterizedTest
  void shouldNotThrowExceptionWhenEnvHasAllowedChars(String env) {
    setEnvProperty(env);
    assertThat(getFolioEnvName()).isEqualTo(env);
  }

  @ParameterizedTest
  @ValueSource(strings = {"!", "@", "%$$#", "def qa"})
  void shouldThrowExceptionWhenEnvHasDisallowedChars(String env) {
    try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
      var validator = validatorFactory.getValidator();
      var folioEnvironment = FolioEnvironment.of(env);
      var validationResponse = validator.validate(folioEnvironment);
      assertThat(validationResponse).isNotEmpty()
        .map(ConstraintViolation::getMessage)
        .containsExactly("Value must follow the pattern: '[\\w\\-]+'");
    }
  }

  private void setEnvProperty(String value) {
    setProperty("env", value);
  }
}
