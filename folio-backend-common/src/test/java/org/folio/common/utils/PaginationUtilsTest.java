package org.folio.common.utils;

import static java.util.List.of;
import static org.apache.commons.collections4.ListUtils.union;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.common.utils.PaginationUtils.loadInBatches;
import static org.folio.common.utils.PaginationUtils.subListAtOffset;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang3.RandomUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PaginationUtilsTest {

  private static final List<String> TEST_LIST = of("a", "b", "c");
  private static final List<String> EMPTY_TEST_LIST = Collections.emptyList();
  private static final Integer BATCH_SIZE = 50;

  @Mock private TestClient queryClient;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(queryClient);
  }

  @Test
  void loadInBatches_positive() {
    var ids = ids(110, 140);

    var firstBatch = ids.subList(0, BATCH_SIZE);
    var secondBatch = ids.subList(BATCH_SIZE, BATCH_SIZE * 2);
    var thirdBatch = ids.subList(BATCH_SIZE + secondBatch.size(), ids.size());
    var firstBatchResolved = randomSubListOrdered(firstBatch, 30);
    var secondBatchResolved = EMPTY_TEST_LIST;
    var thirdBatchResolved = randomSubListOrdered(thirdBatch, 10);

    when(queryClient.query(firstBatch, BATCH_SIZE)).thenReturn(firstBatchResolved);
    when(queryClient.query(secondBatch, BATCH_SIZE)).thenReturn(secondBatchResolved);
    when(queryClient.query(thirdBatch, BATCH_SIZE)).thenReturn(thirdBatchResolved);

    var result = loadInBatches(ids, idsBatch -> queryClient.query(idsBatch, BATCH_SIZE), BATCH_SIZE);

    assertThat(result).containsExactlyElementsOf(union(firstBatchResolved, thirdBatchResolved));
    verify(queryClient).query(firstBatch, BATCH_SIZE);
    verify(queryClient).query(secondBatch, BATCH_SIZE);
    verify(queryClient).query(thirdBatch, BATCH_SIZE);
  }

  @Test
  void loadInBatches_negative_invalidBatchSize() {
    assertThatThrownBy(() ->
      loadInBatches(List.of("test"), idsBatch -> queryClient.query(idsBatch, 10), 0)
    ).isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Batch size should be >= 1");
    verifyNoInteractions(queryClient);
  }

  @Test
  void loadInBatches_negative_emptyIds() {
    loadInBatches(Collections.emptyList(), idsBatch -> queryClient.query(idsBatch, 10), 10);
    verifyNoInteractions(queryClient);
  }

  @ParameterizedTest
  @MethodSource("subListAtOffsetDataProvider")
  @DisplayName("subListAtOffset_parametrized")
  void subListAtOffset_parametrized(int offset, int limit, List<String> list, List<String> expectedSubList) {
    var subList = subListAtOffset(offset, limit, list);
    assertNotNull(subList);
    assertThat(subList).containsExactlyElementsOf(expectedSubList);
  }

  @Test
  void subListAtOffset_negative_nullableList() {
    var subList = subListAtOffset(0, 1, null);
    assertNull(subList);
  }

  private static Stream<Arguments> subListAtOffsetDataProvider() {
    return Stream.of(
      arguments(0, 1, TEST_LIST, of("a")),
      arguments(0, 1, TEST_LIST, of("a")),
      arguments(1, 1, TEST_LIST, of("b")),
      arguments(2, 1, TEST_LIST, of("c")),
      arguments(3, 1, TEST_LIST, EMPTY_TEST_LIST),
      arguments(0, 2, TEST_LIST, of("a", "b")),
      arguments(1, 2, TEST_LIST, of("b", "c")),
      arguments(2, 2, TEST_LIST, of("c")),
      arguments(0, 3, TEST_LIST, TEST_LIST),
      arguments(0, 1, EMPTY_TEST_LIST, EMPTY_TEST_LIST)
    );
  }

  private List<String> ids(int startInclusive, int endExclusive) {
    return IntStream.range(0, RandomUtils.nextInt(startInclusive, endExclusive))
      .mapToObj(i -> String.format("rec=%d", i)).collect(Collectors.toList());
  }

  private static <T> List<T> randomSubListOrdered(List<T> list, int newSize) {
    List<T> shuffled = new ArrayList<>(list);
    Collections.shuffle(shuffled);
    shuffled = shuffled.subList(0, newSize);
    return list.stream().filter(shuffled::contains).collect(Collectors.toList());
  }

  private static final class TestClient {

    public List<String> query(List<String> ids, int batchSize) {
      return null;
    }
  }
}
