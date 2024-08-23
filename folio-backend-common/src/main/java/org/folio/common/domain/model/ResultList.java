package org.folio.common.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ResultList<T> {

  /**
   * Page number.
   */
  @JsonAlias("total_records")
  private int totalRecords = 0;

  /**
   * Paged result data.
   */
  private List<T> records = Collections.emptyList();

  /**
   * Sets the paged result of generic records to the result field. Used by jackson parser.
   *
   * @param key - required
   * @param result - list with generic records
   */
  @JsonAnySetter
  public void set(@SuppressWarnings("unused") String key, List<T> result) {
    this.records = result;
  }

  /**
   * Checks if result list is empty or not.
   *
   * @return true if result list is empty, false - otherwise
   */
  @JsonIgnore
  public boolean isEmpty() {
    return CollectionUtils.isEmpty(records);
  }

  /**
   * Creates empty result list.
   *
   * @param <R> generic type for result item.
   * @return empty result list.
   */
  public static <R> ResultList<R> empty() {
    return new ResultList<>();
  }

  /**
   * Creates result list from list of result objects.
   *
   * @param records - {@link List} with record values
   * @param <R> generic type for result item.
   * @return empty result list.
   */
  public static <R> ResultList<R> asSinglePage(List<R> records) {
    return new ResultList<>(records.size(), records);
  }

  /**
   * Creates result list from given array of result objects.
   *
   * @param records - array with record values
   * @param <R> generic type for result item.
   * @return empty result list.
   */
  @SafeVarargs
  public static <R> ResultList<R> asSinglePage(R... records) {
    return new ResultList<>(records.length, Arrays.asList(records));
  }
}
