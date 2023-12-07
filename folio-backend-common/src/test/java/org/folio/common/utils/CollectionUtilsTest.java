package org.folio.common.utils;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class CollectionUtilsTest {

  @Test
  void toSafe_positive() {
    var actual = CollectionUtils.toStream(List.of(1, 2)).collect(toList());
    assertThat(actual).containsExactly(1, 2);
  }

  @Test
  void toSafeStream_positive_nullValue() {
    var actual = CollectionUtils.toStream(null).collect(toList());
    assertThat(actual).isEmpty();
  }

  @Test
  void toSafeStream_positive_emptyCollection() {
    var actual = CollectionUtils.toStream(emptyList()).collect(toList());
    assertThat(actual).isEmpty();
  }

  @Test
  void mapItems_positive() {
    var actual = CollectionUtils.mapItems(List.of(1, 2, 3), String::valueOf);
    assertThat(actual).isEqualTo(List.of("1", "2", "3"));
  }

  @DisplayName("reverseList_parameterized")
  @MethodSource("reverseListTestDataProvider")
  @ParameterizedTest
  void reverseList_parameterized(List<Integer> given, List<Integer> expected) {
    var result = CollectionUtils.reverseList(given);
    assertThat(result).isEqualTo(expected);
  }

  private static Stream<Arguments> reverseListTestDataProvider() {
    return Stream.of(
      arguments(null, emptyList()),
      arguments(emptyList(), emptyList()),
      arguments(List.of(1, 2, 3), List.of(3, 2, 1)),
      arguments(List.of(3, 2), List.of(2, 3)),
      arguments(List.of(1), List.of(1))
    );
  }
}
