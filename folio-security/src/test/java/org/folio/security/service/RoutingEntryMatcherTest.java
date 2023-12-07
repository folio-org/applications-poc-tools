package org.folio.security.service;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.test.TestUtils.parse;
import static org.folio.test.TestUtils.readString;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.folio.security.domain.model.descriptor.ModuleDescriptor;
import org.folio.security.domain.model.descriptor.RoutingEntry;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RoutingEntryMatcherTest {

  private static final ModuleDescriptor DESCRIPTOR =
    parse(readString("json/fooDescriptor.json"), ModuleDescriptor.class);

  @Mock private InternalModuleDescriptorProvider descriptorProvider;

  @InjectMocks private RoutingEntryMatcher routingEntryMatcher;

  @ParameterizedTest(name = "[{index}] {0}: {1}")
  @MethodSource("requestDataProvider")
  @DisplayName("lookupForRequest_parameterized")
  void lookupForIngressRequest_parameterized(HttpMethod method, String path, RoutingEntry expected) {
    when(descriptorProvider.getModuleDescriptor()).thenReturn(DESCRIPTOR);

    var methodName = ofNullable(method).map(HttpMethod::name).orElse(null);

    var actual = routingEntryMatcher.lookup(methodName, path);

    assertThat(actual).isEqualTo(ofNullable(expected));
  }

  private static Stream<Arguments> requestDataProvider() {
    var id1 = "00000000-0000-0000-0000-000000000000";
    var id2 = "ffffffff-ffff-ffff-ffff-ffffffffffff";
    return Stream.of(
      arguments(GET, null, null),
      arguments(GET, "/foo/entities", routingEntry("/foo/entities", GET, POST)),
      arguments(POST, "/foo/entities", routingEntry("/foo/entities", GET, POST)),
      arguments(null, "/foo/entities", routingEntry("/foo/entities", GET, POST)),
      arguments(DELETE, "/foo/entities", null),
      arguments(GET, "/foo/entities/", null),
      arguments(GET, "/unknown/entities", null),
      arguments(OPTIONS, "/unknown/entities", routingEntry(null, OPTIONS)),

      arguments(GET, format("/foo/entities/%s", id1), routingEntry("/foo/entities/{id}", GET)),
      arguments(PUT, format("/foo/entities/%s", id1), routingEntry("/foo/entities/{id}", PUT)),
      arguments(POST, format("/foo/entities/%s", id1), null),
      arguments(PATCH, format("/foo/entities/%s", id1), null),
      arguments(OPTIONS, format("/foo/entities/%s", id1), routingEntry(null, OPTIONS)),
      arguments(DELETE, format("/foo/entities/%s", id1), routingEntry("/foo/entities/{id}", DELETE)),

      arguments(PUT, format("/foo/%s/entities", id1), routingEntry("/foo/{id}/entities", PUT, PATCH)),
      arguments(PATCH, format("/foo/%s/entities", id1), routingEntry("/foo/{id}/entities", PUT, PATCH)),

      arguments(GET, format("/foo/%s/sub-entities/%s", id1, id2),
        routingEntry("/foo/{fooId}/sub-entities/{subEntityId}", GET, PUT)),

      arguments(PATCH, format("/foo/%s/sub-entities-2/%s", id1, id2),
        routingEntry("/foo/{foo-id}/sub-entities-2/{sub-entity-id}", PATCH)),

      arguments(GET, "/foo2/entities", routingEntry("/foo2*", "*")),
      arguments(POST, "/foo2/entities", routingEntry("/foo2*", "*")),
      arguments(PUT, format("/foo2/entities/%s", id1), routingEntry("/foo2*", "*")),
      arguments(POST, format("/foo2/%s/entities", id1), routingEntry("/foo2*", "*")),

      // legacy stuff
      arguments(GET, "/foo3/values", routingEntryWithPath("/foo3/values", GET)),
      arguments(POST, "/foo3/values", null),
      arguments(POST, "/foo3/samples", null),
      arguments(GET, "{/", null),
      arguments(GET, "/{}", routingEntry("/{id}", GET)),
      arguments(GET, id1, null),
      arguments(GET, format("%s/", id1), null),
      arguments(GET, format("/%s/", id1), null),
      arguments(GET, format("/%s", id1), routingEntry("/{id}", GET)),
      arguments(GET, format("/%s//%s", id1, id2), routingEntry("/{id1}/*/{id2}", GET)),
      arguments(GET, format("/foo/%s/entities", id1), routingEntry("/{id1}/*/{id2}", GET)),

      // tenant API
      arguments(POST, "/_/tenant", routingEntry("/_/tenant", POST)),
      arguments(GET, "/_/tenant", null),
      arguments(POST, format("/_/tenant/%s", id2), null),
      arguments(GET, format("/_/tenant/%s", id2), routingEntry("/_/tenant/{id}", GET, DELETE)),
      arguments(DELETE, format("/_/tenant/%s", id2), routingEntry("/_/tenant/{id}", GET, DELETE))
    );
  }

  private static RoutingEntry routingEntry(String pathPattern, HttpMethod... httpMethods) {
    var methods = Arrays.stream(httpMethods).map(HttpMethod::name).collect(Collectors.toList());
    return routingEntry(pathPattern, methods);
  }

  private static RoutingEntry routingEntry(String pathPattern, String... httpMethods) {
    return routingEntry(pathPattern, Arrays.asList(httpMethods));
  }

  private static RoutingEntry routingEntry(String pathPattern, List<String> methods) {
    return new RoutingEntry().pathPattern(pathPattern).methods(methods);
  }

  private static RoutingEntry routingEntryWithPath(String path, HttpMethod... httpMethods) {
    var methods = Arrays.stream(httpMethods).map(HttpMethod::name).collect(Collectors.toList());
    return new RoutingEntry().path(path).methods(methods);
  }
}
