package org.folio.common.gateway.utils;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.collections4.CollectionUtils.containsAny;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.common.domain.model.RoutingEntry;
import org.springframework.http.HttpMethod;

/**
 * Gateway-neutral derivation of route identity from FOLIO routing entries.
 *
 * <p>Both gateway integrations hash {@link #buildRouteName} output into the route id, so this derivation is a
 * cross-gateway invariant: changing it changes route identity and forces delete/recreate on the next route-set
 * update.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GatewayRouteUtils {

  private static final List<String> ALL_METHODS = getAllHttpMethods();
  private static final String PATH_VARIABLE_REGEX_GROUP = "([^/]+)";
  private static final Pattern PATH_VARIABLE_REGEX = Pattern.compile("\\{[^}]+}");

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

  /**
   * Converts a routing-entry path with variables or wildcards into an anchored regex, keeping static paths as-is.
   *
   * @param staticPath - request path
   * @return pair of updated path and its priority (0 for regex patterns, 1 for exact paths)
   */
  public static Pair<String, Integer> preparePath(String staticPath) {
    if (StringUtils.containsAny(staticPath, '{', '}', '*')) {
      var pathRegex = PATH_VARIABLE_REGEX.matcher(staticPath)
        .replaceAll(PATH_VARIABLE_REGEX_GROUP)
        .replace("*", "(.*)")
        + "$";
      return Pair.of("^" + pathRegex, 0);
    }

    return Pair.of(staticPath, 1);
  }

  /**
   * Builds the deterministic route-name string that gateway integrations hash into the route identifier.
   *
   * @param moduleId - module identifier
   * @param interfaceId - interface identifier including version
   * @param path - prepared path from {@link #preparePath}
   * @param httpMethods - http method names
   * @return route-name string joining the non-blank components with {@code |}
   */
  public static String buildRouteName(String moduleId, String interfaceId, String path, List<String> httpMethods) {
    return Stream.of(path, String.join(",", httpMethods), moduleId, interfaceId)
      .filter(StringUtils::isNotBlank)
      .collect(joining("|"));
  }

  private static List<String> getAllHttpMethods() {
    return stream(HttpMethod.values())
      .map(HttpMethod::name)
      .toList();
  }
}
