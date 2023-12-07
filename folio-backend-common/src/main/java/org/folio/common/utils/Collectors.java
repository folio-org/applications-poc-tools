package org.folio.common.utils;

import static java.util.stream.Collectors.toMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Collectors {

  /**
   * Returns a Collector that accumulates the input elements into a new {@link LinkedHashMap} in encounter order.
   *
   * @param keyFunc - key mapper as {@link Function} object
   * @param <K> - generic type for map keys
   * @param <V> - generic type for map values
   * @return a {@link Collector} which collects all the input elements into a {@link LinkedHashMap} in encounter order
   */
  public static <K, V> Collector<V, ?, Map<K, V>> toLinkedHashMap(Function<V, K> keyFunc) {
    return toLinkedHashMap(keyFunc, Function.identity());
  }

  /**
   * Returns a Collector that accumulates the input elements into a new {@link LinkedHashMap} in encounter order.
   *
   * @param keyFunc - key mapper as {@link Function} object
   * @param valueFunc - value mapper as {@link Function} object
   * @param <T> - generic type for input values
   * @param <K> - generic type for map keys
   * @param <V> - generic type for map values
   * @return a {@link Collector} which collects all the input elements into a {@link LinkedHashMap} in encounter order
   */
  public static <T, K, V> Collector<T, ?, Map<K, V>> toLinkedHashMap(Function<T, K> keyFunc, Function<T, V> valueFunc) {
    return toMap(keyFunc, valueFunc, (o, n) -> n, LinkedHashMap::new);
  }
}
