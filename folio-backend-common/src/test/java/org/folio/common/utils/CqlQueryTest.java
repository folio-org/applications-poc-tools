package org.folio.common.utils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.CqlQuery.exactMatchAny;

import java.util.List;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class CqlQueryTest {

  private static final String TEST_PARAM = "name";

  @Test
  void exactMatchAny_positive_oneValue() {
    assertThat(exactMatchAny(TEST_PARAM, List.of("id1")))
      .hasToString("name==(\"id1\")");
  }

  @Test
  void exactMatchAny_positive_moreThanOneValue() {
    assertThat(exactMatchAny(TEST_PARAM, asList("id1", "id2")))
      .hasToString("name==(\"id1\" or \"id2\")");
  }

  @Test
  void exactMatchAny_shouldFilterOutEmptyString() {
    assertThat(exactMatchAny(TEST_PARAM, asList("id1", null, "")))
      .hasToString("name==(\"id1\")");
  }
}
