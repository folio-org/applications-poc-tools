package org.folio.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class InterfaceReferenceTest {

  @ParameterizedTest(name = "[{index}] id=''{0}-{1}'', isCompatible={2}")
  @CsvSource({
    "test-interface, 0.1, false",
    "test-interface, 1.1, false",
    "test-interface, 1.2, false",
    "test-interface, 1.3, true",
    "test-interface, 1.3 3.0, true",
    "test-interface, 2.0, true",
    "test-interface, 2.1, true",
    "test-interface, 3.0, false",
    "bar-interface, 2.0, false"
  })
  void isCompatible_parameterized(String name, String version, boolean expected) {
    var given = InterfaceReference.of(name, version);
    var interfaceToCompare = InterfaceReference.of("test-interface", "1.2.3 2.0");
    var comparisonResult = given.isCompatible(interfaceToCompare);
    assertThat(comparisonResult).isEqualTo(expected);
  }
}

