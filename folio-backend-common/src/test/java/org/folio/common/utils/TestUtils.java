package org.folio.common.utils;

import static java.util.stream.Collectors.toCollection;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TestUtils {

  @SuppressWarnings("unchecked")
  public static <K, V> Map<K, V> mapOf(K k1, V v1, Object... pairs) {
    Map<K, V> map = new LinkedHashMap<>();
    map.put(k1, v1);
    for (int i = 0; i < pairs.length; i += 2) {
      Object key = pairs[i];
      Object value = pairs[i + 1];
      map.put((K) key, (V) value);
    }
    return map;
  }

  @SafeVarargs
  public static <T> Set<T> setOf(T... values) {
    return Arrays.stream(values).collect(toCollection(LinkedHashSet::new));
  }

  @SafeVarargs
  public static <T> T[] array(T... values) {
    return values;
  }
}
