package org.folio.tools.kong.model.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tools.kong.model.expression.RouteExpressions.httpHeader;
import static org.folio.tools.kong.model.expression.RouteExpressions.httpMethod;
import static org.folio.tools.kong.model.expression.RouteExpressions.httpPath;
import static org.folio.tools.kong.model.expression.RouteExpressions.netProtocol;
import static org.folio.tools.kong.model.expression.RouteExpressions.queryParameter;
import static org.folio.tools.kong.model.expression.RouteExpressions.tlsSni;
import static org.folio.tools.kong.model.transformation.StringTransformations.LOWER;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class StringExpressionTest {

  @DisplayName("toString_parameterized")
  @MethodSource("stringExpressionDataProvider")
  @ParameterizedTest(name = "[{index}] given=''{0}'', expected=''{1}''")
  void toString_parameterized(RouteExpression stringExpression, String expected) {
    var result = stringExpression.toString();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void toString_nullValue() {
    var builder = httpPath();
    assertThatThrownBy(() -> builder.equalsTo(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("StringExpression value must not be null");
  }

  @Test
  void toString_nullRegex() {
    var builder = httpPath();
    assertThatThrownBy(() -> builder.regexMatching(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("StringExpression regex must not be null");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "x okapi tenant", "x-okapi tenant"})
  void toString_invalidHeaderName(String headerName) {
    assertThatThrownBy(() -> httpHeader(headerName))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid header value, must not be blank and must not contain any whitespaces: " + headerName);
  }

  private static Stream<Arguments> stringExpressionDataProvider() {
    return Stream.of(
      arguments(httpPath().equalsTo("/sample"), "http.path == \"/sample\""),
      arguments(httpPath().notEqualsTo("/sample"), "http.path != \"/sample\""),
      arguments(httpPath().prefixMatching("/sample"), "http.path ^= \"/sample\""),
      arguments(httpPath().suffixMatching("/sample"), "http.path =^ \"/sample\""),
      arguments(httpPath().regexMatching("/sample[1-9]+"), "http.path ~ \"/sample[1-9]+\""),
      arguments(httpPath().contains("sample-value"), "http.path contains \"sample-value\""),
      arguments(httpPath().withTransformation(LOWER).equalsTo("/sample"), "lower(http.path) == \"/sample\""),

      arguments(httpHeader("Authorization").equalsTo("test"), "http.headers.authorization == \"test\""),
      arguments(httpHeader("x-okapi-tenant").equalsTo("test"), "http.headers.x_okapi_tenant == \"test\""),

      arguments(netProtocol().equalsTo("http"), "net.protocol == \"http\""),
      arguments(tlsSni().equalsTo("sample"), "tls.sni == \"sample\""),
      arguments(httpMethod().equalsTo("GET"), "http.method == \"GET\""),
      arguments(queryParameter("limit").equalsTo("10"), "http.queries.limit == \"10\"")
    );
  }
}
