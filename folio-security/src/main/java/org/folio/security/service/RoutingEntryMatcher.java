package org.folio.security.service;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.common.domain.model.InterfaceDescriptor;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.domain.model.RoutingEntry;

@RequiredArgsConstructor
public class RoutingEntryMatcher {

  private final InternalModuleDescriptorProvider descriptorProvider;

  public Optional<RoutingEntry> lookup(String method, String path) {
    var descriptor = descriptorProvider.getModuleDescriptor();
    var map = getAllRoutingEntries(descriptor);

    if (MapUtils.isEmpty(map) || path == null) {
      return Optional.empty();
    }

    var tryPath = path;
    while (true) {
      var candidateInstances = map.get(tryPath);
      if (candidateInstances != null) {
        for (var candidate : candidateInstances) {
          if (match(candidate, path, method)) {
            return Optional.of(candidate);
          }
        }
      }

      int index = tryPath.lastIndexOf('/', tryPath.length() - 2);
      if (index < 0) {
        break;
      }
      tryPath = tryPath.substring(0, index + 1);
    }
    return Optional.empty();
  }

  private static Map<String, List<RoutingEntry>> getAllRoutingEntries(ModuleDescriptor descriptor) {
    var provides = descriptor.getProvides().stream()
      .collect(toMap(InterfaceDescriptor::getId, InterfaceDescriptor::getHandlers));

    var map = new HashMap<String, List<RoutingEntry>>();

    for (var mapEntries : provides.entrySet()) {
      for (var routingEntry : mapEntries.getValue()) {
        var prefix = getPatternPrefix(routingEntry);
        var list = map.computeIfAbsent(prefix, k -> new ArrayList<>());

        list.add(routingEntry);
      }
    }

    return map;
  }

  private static boolean match(RoutingEntry re, String uri, String method) {
    var methods = emptyIfNull(re.getMethods());
    for (var m : methods) {
      if (method == null || m.equals("*") || m.equals(method)) {
        return matchUri(re, uri);
      }
    }
    return false;
  }

  private static boolean matchUri(RoutingEntry re, String path) {
    var pathPattern = re.getPathPattern();
    if (pathPattern != null) {
      return fastMatch(pathPattern, path);
    }

    return re.getPath() == null || path.startsWith(re.getPath());
  }

  private static boolean fastMatch(String pathPattern, String path) {
    return fastMatch(pathPattern, 0, path, 0, path.length());
  }

  /**
   * This method has been copied from okapi, so 'Cognitive Complexity of methods should not be too high' sonarcloud
   * issues suppressed for now.
   */
  @SuppressWarnings("java:S3776")
  private static boolean fastMatch(String pathPattern, int patternIndex, String path, int uriIndex, int pathLength) {
    while (patternIndex < pathPattern.length()) {
      var patternChar = pathPattern.charAt(patternIndex);
      patternIndex++;
      if (patternChar == '{') {
        while (true) {
          if (pathPattern.charAt(patternIndex) == '}') {
            patternIndex++;
            break;
          }
          patternIndex++;
        }
        var empty = true;
        while (uriIndex < pathLength && path.charAt(uriIndex) != '/') {
          uriIndex++;
          empty = false;
        }
        if (empty) {
          return false;
        }
      } else if (patternChar != '*') {
        if (uriIndex == pathLength || patternChar != path.charAt(uriIndex)) {
          return false;
        }
        uriIndex++;
      } else {
        do {
          if (fastMatch(pathPattern, patternIndex, path, uriIndex, pathLength)) {
            return true;
          }
          uriIndex++;
        } while (uriIndex <= pathLength);
        return false;
      }
    }
    return uriIndex == pathLength;
  }

  private static String getPatternPrefix(RoutingEntry endpoint) {
    var pathPattern = StringUtils.getIfEmpty(endpoint.getPath(), endpoint::getPathPattern);
    if (pathPattern == null) {
      return "/";
    }

    var lastSlash = 0;
    for (var i = 0; i < pathPattern.length(); i++) {
      switch (pathPattern.charAt(i)) {
        case '*', '{':
          return pathPattern.substring(0, lastSlash);
        case '/':
          lastSlash = i + 1;
          break;
        default:
          break;
      }
    }
    return pathPattern;
  }
}
