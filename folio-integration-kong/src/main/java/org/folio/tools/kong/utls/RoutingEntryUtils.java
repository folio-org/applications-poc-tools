package org.folio.tools.kong.utls;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.containsAny;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.common.domain.model.RoutingEntry;
import org.springframework.http.HttpMethod;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RoutingEntryUtils {

  private static final List<String> ALL_METHODS = getAllHttpMethods();

  /**
   * Provides a list with http method names for {@link RoutingEntry} object.
   *
   * @param re - routing entry to process
   * @return {@link List} with {@link String} method names
   */
  public static List<String> getMethods(RoutingEntry re) {
    var methods = re.getMethods();
    if (CollectionUtils.isEmpty(methods)) {
      return emptyList();
    }

    return containsAny(methods, "*") ? ALL_METHODS : methods;
  }

  private static List<String> getAllHttpMethods() {
    return stream(HttpMethod.values())
      .map(HttpMethod::name)
      .toList();
  }
}
