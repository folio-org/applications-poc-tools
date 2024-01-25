package org.folio.tools.kong.model.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.tools.kong.model.expression.RouteExpressions.bool;
import static org.folio.tools.kong.model.expression.RouteExpressions.combineUsingAnd;
import static org.folio.tools.kong.model.expression.RouteExpressions.combineUsingOr;
import static org.folio.tools.kong.model.expression.RouteExpressions.httpMethod;
import static org.folio.tools.kong.model.expression.RouteExpressions.netPort;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class BoolExpressionTest {

  @DisplayName("toString_parameterized")
  @MethodSource("boolExpressionDataProvider")
  @ParameterizedTest(name = "[{index}] given=''{0}'', expected=''{1}''")
  void toString_parameterized(RouteExpression stringExpression, String expected) {
    var result = stringExpression.toString();
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void toString_rightValueIsNullInOr() {
    var left = httpMethod().equalsTo("GET");
    var builder = bool(left);
    assertThatThrownBy(() -> builder.or(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Right value of BooleanExpression must not be null");
  }

  @Test
  void toString_leftValueIsNullInOr() {
    var right = httpMethod().equalsTo("GET");
    var builder = bool(null);
    assertThatThrownBy(() -> builder.or(right))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Left value of BooleanExpression must not be null");
  }

  private static Stream<Arguments> boolExpressionDataProvider() {
    return Stream.of(
      arguments(
        bool(netPort().equalsTo(8080)).or(httpMethod().equalsTo("GET")),
        "(net.port == 8080 || http.method == \"GET\")"),

      arguments(
        bool(netPort().equalsTo(8080)).and(httpMethod().equalsTo("GET")),
        "(net.port == 8080 && http.method == \"GET\")"),

      arguments(
        bool(bool(netPort().equalsTo(8080)).or(httpMethod().equalsTo("GET"))).or(httpMethod().equalsTo("POST")),
        "((net.port == 8080 || http.method == \"GET\") || http.method == \"POST\")"),

      arguments(
        bool(bool(netPort().equalsTo(8080)).or(httpMethod().equalsTo("GET"))).and(httpMethod().equalsTo("POST")),
        "((net.port == 8080 || http.method == \"GET\") && http.method == \"POST\")"),

      arguments(combineUsingOr(httpMethod().equalsTo("GET")), "http.method == \"GET\""),

      arguments(
        combineUsingOr(httpMethod().equalsTo("GET"), httpMethod().equalsTo("POST"), httpMethod().equalsTo("PUT")),
        "(http.method == \"GET\" || http.method == \"POST\" || http.method == \"PUT\")"),

      arguments(
        combineUsingOr(List.of(httpMethod().equalsTo("GET"), httpMethod().equalsTo("POST"))),
        "(http.method == \"GET\" || http.method == \"POST\")"),

      arguments(combineUsingAnd(httpMethod().equalsTo("GET")), "http.method == \"GET\""),

      arguments(
        combineUsingAnd(List.of(httpMethod().equalsTo("GET"), httpMethod().equalsTo("POST"))),
        "(http.method == \"GET\" && http.method == \"POST\")"),

      arguments(
        combineUsingAnd(httpMethod().equalsTo("GET"), httpMethod().equalsTo("POST"), httpMethod().equalsTo("PUT")),
        "(http.method == \"GET\" && http.method == \"POST\" && http.method == \"PUT\")")
    );
  }
}
