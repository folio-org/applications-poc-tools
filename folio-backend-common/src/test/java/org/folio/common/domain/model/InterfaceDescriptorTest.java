package org.folio.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class InterfaceDescriptorTest {

  @ParameterizedTest(name = "[{index}] id1=''{0}-{1}'', id2=''{2}-{3}'', result={4}")
  @CsvSource({
    "bar-interface, 1.2, test-interface, 1.2, 0x7fffffff",
    "test-interface, 1.2, test-interface, 2.1, 0x7fffffff",
    "test-interface, 1.2.3, test-interface, 1.2.3, 0",
    "test-interface, 1.2.3, test-interface, 1.2, 1",
    "test-interface, 1.2.3, test-interface, 1.3, -2",
    "test-interface, 1.3, test-interface, 1.2.3, 2",
    "test-interface, 1.3, test-interface, 1.2, 2",

    "test-interface, 1.3 2.0, test-interface, 1.2, 2",
    "test-interface, 1.3 2.0, test-interface, 1.3, 0",
    "test-interface, 1.3 2.0, test-interface, 1.3.1, -1",
    "test-interface, 1.3 2.0, test-interface, 2.0, 0x7fffffff",
    "test-interface, 1.3 2.0, test-interface, 2.1, 0x7fffffff",

    "test-interface, 1.3, test-interface, 1.3 2.0, 0",
    "test-interface, 2.0, test-interface, 1.3 2.0, 0",
    "test-interface, 2.0.1, test-interface, 1.3 2.0, 1",
    "test-interface, 1.2, test-interface, 1.3 2.0, -2",
    "test-interface, 2.1, test-interface, 1.3 2.0, 2",
  })
  void compare_parameterized(String name1, String version1, String name2, String version2, int expectedResult) {
    var interfaceDescriptor1 = new InterfaceDescriptor(name1, version1);
    var interfaceDescriptor2 = new InterfaceDescriptor(name2, version2);

    assertThat(interfaceDescriptor1.compare(interfaceDescriptor2)).isEqualTo(expectedResult);
  }

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
    var given = new InterfaceDescriptor(name, version);
    var interfaceToCompare = new InterfaceDescriptor("test-interface", "1.2.3 2.0");
    var comparisonResult = given.isCompatible(interfaceToCompare);
    assertThat(comparisonResult).isEqualTo(expected);
  }

  @ParameterizedTest(name = "[{index}] id=''{0}-{1}'', isTimer={2}")
  @CsvSource({
    "_timer, 1.0, true",
    "_TimeR, 1.0, false",
    "timer, 1.0, false",
    "test-interface, 1.0, false"
  })
  void isTimer_parameterized(String name, String version, boolean expected) {
    var given = new InterfaceDescriptor(name, version);
    var actual = given.isTimer();
    assertThat(actual).isEqualTo(expected);
  }
}
