package org.folio.common.utils;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectionUtils {

  /**
   * Returns a stream from nullable collection.
   *
   * @param source - nullable {@link Collection} object
   * @param <T> - generic type for collection element
   * @return a stream from nullable collection
   */
  public static <T> Stream<T> toStream(Collection<T> source) {
    return emptyIfNull(source).stream();
  }

  /**
   * Converts elements in a nullable collection using mapper function.
   *
   * @param source - nullable {@link Collection} object
   * @param mapper - value mapper {@link Function} object
   * @param <T> - generic type of collection element
   * @param <R> - generic type for response list element
   * @return a list with converted items
   */
  public static <T, R> List<R> mapItems(Collection<T> source, Function<? super T, ? extends R> mapper) {
    return toStream(source).map(mapper).collect(toList());
  }

  /**
   * Creates a new list with reversed order of elements from a given list.
   *
   * @param given - a list to reverse, nullable.
   * @param <T> - generic type for element in the given and result list
   * @return a new list with reversed order of elements.
   */
  public static <T> List<T> reverseList(List<T> given) {
    if (given == null) {
      return emptyList();
    }

    var result = new ArrayList<T>();
    for (int i = given.size() - 1; i >= 0; i--) {
      result.add(given.get(i));
    }

    return result;
  }

  /**
   * Collects given collection to set using mapping function.
   *
   * @param source - source collection to process
   * @param mapper - java {@link Function} mapping function
   * @param <T> - generic type for incoming collection element
   * @param <R> - generic type for output collection element
   * @return - created {@link Set} object
   */
  public static <T, R> Set<R> mapItemsToSet(Collection<T> source, Function<? super T, ? extends R> mapper) {
    return toStream(source).map(mapper).collect(toSet());
  }

  public static <T> T takeOne(Collection<T> source) {
    return takeOne(source,
      () -> new NoSuchElementException("Collection is empty"),
      () -> new NoSuchElementException("Collection contains more than one element: count = " + source.size()));
  }

  public static <T> T takeOne(Collection<T> source, Supplier<? extends RuntimeException> emptyCollectionExcSupplier,
    Supplier<? extends RuntimeException> tooManyItemsExcSupplier) {
    if (isEmpty(source)) {
      throw emptyCollectionExcSupplier.get();
    }

    if (source.size() > 1) {
      throw tooManyItemsExcSupplier.get();
    }

    return source.iterator().next();
  }

  public static <T> Optional<T> findOne(Collection<T> source) {
    return emptyIfNull(source).size() == 1 ? Optional.ofNullable(source.iterator().next()) : Optional.empty();
  }
}
