package org.folio.tools.kong.model.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tools.kong.model.expression.RouteExpressions.netDstPort;
import static org.folio.tools.kong.model.expression.RouteExpressions.netPort;
import static org.folio.tools.kong.model.expression.RouteExpressions.netSrcPort;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class IntExpressionTest {

  @DisplayName("toString_parameterized")
  @MethodSource("intExpressionDataProvider")
  @ParameterizedTest(name = "[{index}] given=''{0}'', expected=''{1}''")
  void toString_parameterized(RouteExpression stringExpression, String expected) {
    var result = stringExpression.toString();
    assertThat(result).isEqualTo(expected);
  }

  private static Stream<Arguments> intExpressionDataProvider() {
    return Stream.of(
      arguments(netPort().equalsTo(8080), "net.port == 8080"),
      arguments(netPort().notEqualsTo(8080), "net.port != 8080"),
      arguments(netPort().greaterThan(8080), "net.port > 8080"),
      arguments(netPort().greaterThanOrEqualsTo(8080), "net.port >= 8080"),
      arguments(netPort().lessThan(8080), "net.port < 8080"),
      arguments(netPort().lessThanOrEqualsTo(8080), "net.port <= 8080"),

      arguments(netSrcPort().equalsTo(8080), "net.src.port == 8080"),
      arguments(netSrcPort().notEqualsTo(8080), "net.src.port != 8080"),
      arguments(netSrcPort().greaterThan(8080), "net.src.port > 8080"),
      arguments(netSrcPort().greaterThanOrEqualsTo(8080), "net.src.port >= 8080"),
      arguments(netSrcPort().lessThan(8080), "net.src.port < 8080"),
      arguments(netSrcPort().lessThanOrEqualsTo(8080), "net.src.port <= 8080"),

      arguments(netDstPort().equalsTo(8080), "net.dst.port == 8080"),
      arguments(netDstPort().notEqualsTo(8080), "net.dst.port != 8080"),
      arguments(netDstPort().greaterThan(8080), "net.dst.port > 8080"),
      arguments(netDstPort().greaterThanOrEqualsTo(8080), "net.dst.port >= 8080"),
      arguments(netDstPort().lessThan(8080), "net.dst.port < 8080"),
      arguments(netDstPort().lessThanOrEqualsTo(8080), "net.dst.port <= 8080")
    );
  }
}
