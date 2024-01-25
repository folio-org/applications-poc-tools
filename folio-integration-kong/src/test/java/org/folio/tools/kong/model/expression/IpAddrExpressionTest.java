package org.folio.tools.kong.model.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tools.kong.model.expression.RouteExpressions.netDstIp;
import static org.folio.tools.kong.model.expression.RouteExpressions.netSrcIp;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@UnitTest
class IpAddrExpressionTest {

  @DisplayName("toString_parameterized")
  @MethodSource("ipAddrExpressionDataProvider")
  @ParameterizedTest(name = "[{index}] given=''{0}'', expected=''{1}''")
  void toString_parameterized(RouteExpression stringExpression, String expected) {
    var result = stringExpression.toString();
    assertThat(result).isEqualTo(expected);
  }

  private static Stream<Arguments> ipAddrExpressionDataProvider() {
    return Stream.of(
      arguments(netSrcIp().equalsTo("0.0.0.0"), "net.src.ip == 0.0.0.0"),
      arguments(netSrcIp().notEqualsTo("0.0.0.0"), "net.src.ip != 0.0.0.0"),
      arguments(netSrcIp().in("0.0.0.0"), "net.src.ip in 0.0.0.0"),
      arguments(netSrcIp().notIn("0.0.0.0"), "net.src.ip not in 0.0.0.0"),

      arguments(netDstIp().equalsTo("0.0.0.0"), "net.dst.ip == 0.0.0.0"),
      arguments(netDstIp().notEqualsTo("0.0.0.0"), "net.dst.ip != 0.0.0.0"),
      arguments(netDstIp().in("0.0.0.0"), "net.dst.ip in 0.0.0.0"),
      arguments(netDstIp().notIn("0.0.0.0"), "net.dst.ip not in 0.0.0.0")
    );
  }
}
