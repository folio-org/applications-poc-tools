package org.folio.common.utils;

import static java.util.stream.Collectors.joining;
import static java.util.stream.StreamSupport.stream;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@RequiredArgsConstructor
@EqualsAndHashCode
public final class CqlQuery {

  private final String query;

  /**
   * Provides CQL query to exact match by any of provided values as {@link Iterable} object.
   *
   * @param index - field to search
   * @param values - list of optional values in field
   * @return - generated {@link CqlQuery} object
   */
  public static CqlQuery exactMatchAny(String index, Iterable<String> values) {
    var valuesConcatenated = stream(values.spliterator(), false)
      .filter(StringUtils::isNotBlank)
      .map(value -> "\"" + value + "\"")
      .collect(joining(" or "));

    return fromTemplate("%s==(%s)", index, valuesConcatenated);
  }

  private static CqlQuery fromTemplate(String format, Object... args) {
    return new CqlQuery(String.format(format, args));
  }

  @Override
  public String toString() {
    return query;
  }
}
