package org.folio.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.Collectors.toLinkedHashMap;
import static org.folio.common.utils.TestUtils.mapOf;

import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.stream.Stream;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CollectorsTest {

  @Test
  void toLinkedHashMap_positive() {
    var actual = Stream.of(1, 1, 2, 2).collect(toLinkedHashMap(Function.identity(), String::valueOf));
    assertThat(actual).isInstanceOf(LinkedHashMap.class).isEqualTo(mapOf(1, "1", 2, "2"));
  }

  @Test
  void toLinkedHashMap_positive_keyMapperOnly() {
    var actual = Stream.of(1, 1, 2, 2).collect(toLinkedHashMap(Function.identity()));
    assertThat(actual).isInstanceOf(LinkedHashMap.class).isEqualTo(mapOf(1, 1, 2, 2));
  }
}
