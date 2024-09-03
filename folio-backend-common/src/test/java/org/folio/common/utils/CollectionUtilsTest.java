package org.folio.common.utils;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.folio.common.utils.CollectionUtils.findOne;
import static org.folio.common.utils.CollectionUtils.mapItemsToSet;
import static org.folio.common.utils.CollectionUtils.takeOne;
import static org.instancio.Select.root;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.folio.test.types.UnitTest;
import org.instancio.Instancio;
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

  @ParameterizedTest(name = "[{index}] input={0}, expected={2}")
  @MethodSource("provideMapItemsToSetArguments")
  <T, R> void mapItemsToSet_positive1(Collection<T> input, Function<? super T, ? extends R> mapper, Set<R> expected) {
    var actual = mapItemsToSet(input, mapper);
    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("provideTakeOneArguments")
  <T> void takeOne_positive(Collection<T> input, T expected) {
    var actual = takeOne(input);
    assertThat(actual).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("provideTakeOneNegativeArguments")
  <T> void takeOne_negative(Collection<T> input, String errMsg) {
    assertThatExceptionOfType(NoSuchElementException.class)
      .isThrownBy(() -> takeOne(input))
      .withMessage(errMsg);
  }

  @ParameterizedTest
  @MethodSource("provideFindOneArguments")
  <T> void findOne_positive(Collection<T> input, Optional<T> expected) {
    var actual = findOne(input);
    assertThat(actual).isEqualTo(expected);
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

  private static Stream<Arguments> provideMapItemsToSetArguments() {
    var list = Instancio.ofList(TestObject.class)
      .generate(root(), gen -> gen.collection().nullableElements().minSize(2).maxSize(20))
      .create();
    list.addAll(list); // duplicated each item

    var set = new HashSet<>(list);

    return Stream.of(
      arguments(null, identity(), emptySet()),
      arguments(emptyList(), identity(), emptySet()),
      arguments(list, identity(), set)
    );
  }

  private static Stream<Arguments> provideTakeOneArguments() {
    var testObject = testObject();

    return Stream.of(
      arguments(testListWithOneNull(), null),
      arguments(List.of(testObject), testObject)
    );
  }

  public static Stream<Arguments> provideTakeOneNegativeArguments() {
    var testObjectList = testListWithMinSize2();

    return Stream.of(
      arguments(null, "Collection is empty"),
      arguments(emptyList(), "Collection is empty"),
      arguments(testObjectList, "Collection contains more than one element: count = " + testObjectList.size())
    );
  }

  private static Stream<Arguments> provideFindOneArguments() {
    var testObject = testObject();

    return Stream.of(
      arguments(null, empty()),
      arguments(emptyList(), empty()),
      arguments(testListWithOneNull(), empty()),
      arguments(List.of(testObject), Optional.of(testObject)),
      arguments(testListWithMinSize2(), empty())
    );
  }

  private static TestObject testObject() {
    return Instancio.create(TestObject.class);
  }

  private static ArrayList<TestObject> testListWithOneNull() {
    var listWithNull = new ArrayList<TestObject>();
    listWithNull.add(null);
    return listWithNull;
  }

  private static List<TestObject> testListWithMinSize2() {
    return Instancio.ofList(TestObject.class)
      .generate(root(), gen -> gen.collection()
        .nullableElements()
        .minSize(2)
        .maxSize(100))
      .create();
  }

  record TestObject(String name, int value) {}
}
