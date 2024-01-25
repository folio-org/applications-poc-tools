package org.folio.tools.kong.model.expression;

import static java.util.Objects.requireNonNull;
import static org.folio.tools.kong.model.operator.IpAddressOperator.EQUALS;
import static org.folio.tools.kong.model.operator.IpAddressOperator.IN;
import static org.folio.tools.kong.model.operator.IpAddressOperator.NOT_EQUALS;
import static org.folio.tools.kong.model.operator.IpAddressOperator.NOT_IN;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.folio.tools.kong.model.operator.RouteOperator;

@Data
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class IpAddrExpressionBuilder {

  static final String NET_SRC_IP_FIELD = "net.src.ip";
  static final String NET_DST_IP_FIELD = "net.dst.ip";
  private String key;

  /**
   * Creates a {@link RouteExpression} for state when field must be equal to an ip address.
   *
   * @param value - string value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression equalsTo(String value) {
    return buildRouteExpression(key, EQUALS, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must not be equal to an ip address.
   *
   * @param value - string value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression notEqualsTo(String value) {
    return buildRouteExpression(key, NOT_EQUALS, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must be in Classless Inter-Domain Routing.
   *
   * @param value - string value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression in(String value) {
    return buildRouteExpression(key, IN, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must not be in Classless Inter-Domain Routing.
   *
   * @param value - string value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression notIn(String value) {
    return buildRouteExpression(key, NOT_IN, value);
  }

  private static RouteExpression buildRouteExpression(String key, RouteOperator operator, String value) {
    requireNonNull(value, "StringExpression value must not be null");
    return new RouteExpression(key, operator, value);
  }
}
