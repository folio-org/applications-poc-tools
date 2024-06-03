package org.folio.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.stream.Stream;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class SemverUtilsTest {

  @ParameterizedTest
  @MethodSource("applicationVersions")
  void getApplicationVersion_validInput_test(String applicationId, String expectedVersion) {
    String actualVersion = SemverUtils.getVersion(applicationId);
    assertEquals(expectedVersion, actualVersion);
  }

  @ParameterizedTest
  @MethodSource("applicationNames")
  void getApplicationName_validInput_test(String applicationId, String expectedName) {
    String actualName = SemverUtils.getName(applicationId);
    assertEquals(expectedName, actualName);
  }

  @Test
  void getApplicationVersion_emptyOrNullInput_test() {
    assertThrows(IllegalArgumentException.class, () -> SemverUtils.getVersion(null));
    assertThrows(IllegalArgumentException.class, () -> SemverUtils.getVersion(""));
  }

  @Test
  void getApplicationName_emptyOrNullInput_test() {
    assertThrows(IllegalArgumentException.class, () -> SemverUtils.getName(null));
    assertThrows(IllegalArgumentException.class, () -> SemverUtils.getName(""));
  }

  @ParameterizedTest
  @MethodSource("satisfiesPositive")
  void satisfies_positive(String version, String rangeOrVersion, boolean expected) {
    var result = SemverUtils.satisfies(version, rangeOrVersion);
    assertEquals(expected, result);
  }

  @ParameterizedTest
  @MethodSource("satisfiesNegative")
  void satisfies_negative_invalidVersion(String version, String rangeOrVersion) {
    assertThrows(IllegalArgumentException.class,
      () -> SemverUtils.satisfies(version, rangeOrVersion),
      () -> "Invalid semantic version: " + version);
  }

  @ParameterizedTest
  @MethodSource("applicationSatisfiesPositive")
  void applicationSatisfies_positive(String applicationId, String rangeOrVersion, boolean expected) {
    var result = SemverUtils.applicationSatisfies(applicationId, rangeOrVersion);
    assertEquals(expected, result);
  }

  private static Stream<Arguments> applicationVersions() {
    return Stream.of(
      of("App-1.0.0", "1.0.0"),
      of("App-1.2.3-release", "1.2.3-release"),
      of("App-1.2.3-release+metadata", "1.2.3-release+metadata"),
      of("AppName-0.0.1", "0.0.1"),
      of("AppName-0.0.1+build", "0.0.1+build")
    );
  }

  private static Stream<Arguments> applicationNames() {
    return Stream.of(
      of("App-1.0.0", "App"),
      of("App-1.2.3-release", "App"),
      of("App-1.2.3-release+metadata", "App"),
      of("AppName-0.0.1", "AppName"),
      of("AppName-0.0.1+build", "AppName")
    );
  }

  private static Stream<Arguments> satisfiesPositive() {
    return Stream.of(
      of("1.0.0", "1.0.0", true),
      of("0.0.1", "~0.0.1", true),
      of("0.0.1", "^0.0.1", true),
      of("0.0.1+build", ">=0.0.1", true),
      of("0.0.1+build", ">=0.0.0 <2.0.0", true),
      of("1.0.0", "1.1.0", false),
      of("1.2.3-release", "1.3.*", false),
      of("1.2.3-release+metadata", "1.3.x", false),
      of("0.0.1", "~0.0.2", false),
      of("0.0.1", "^0.0.2", false),
      of("0.0.1+build", ">=0.0.2", false),
      of("0.0.1+build", ">=1.0.0 <2.0.0", false)
    );
  }

  private static Stream<Arguments> satisfiesNegative() {
    return Stream.of(
      of("1.0.sq", "1.0.0"),
      of("abc", "~0.0.1"),
      of("x.x.x", "^0.0.1"),
      of("app-0.0.1+build", ">=0.0.1"),
      of("0.0.1+", ">=0.0.0 <2.0.0"),
      of("12...0.0", "1.1.0")
    );
  }

  private static Stream<Arguments> applicationSatisfiesPositive() {
    return Stream.of(
      of("app-foo-1.0.0", "1.0.0", true),
      of("app-foo-0.0.1", "~0.0.1", true),
      of("app-foo-0.0.1", "^0.0.1", true),
      of("app-foo-0.0.1+build", ">=0.0.1", true),
      of("app-foo-0.0.1+build", ">=0.0.0 <2.0.0", true),
      of("app-foo-1.0.0", "1.1.0", false),
      of("app-foo-1.2.3-release", "1.3.*", false),
      of("app-foo-1.2.3-release+metadata", "1.3.x", false),
      of("app-foo-0.0.1", "~0.0.2", false),
      of("app-foo-0.0.1", "^0.0.2", false),
      of("app-foo-0.0.1+build", ">=0.0.2", false),
      of("app-foo-0.0.1+build", ">=1.0.0 <2.0.0", false)
    );
  }
}
