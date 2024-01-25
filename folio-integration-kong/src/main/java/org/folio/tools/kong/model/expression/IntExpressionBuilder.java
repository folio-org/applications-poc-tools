package org.folio.tools.kong.model.expression;

import static org.folio.tools.kong.model.operator.IntOperator.GREATER_THAN;
import static org.folio.tools.kong.model.operator.IntOperator.GREATER_THAN_OR_EQUAL;
import static org.folio.tools.kong.model.operator.IntOperator.LESS_THAN;
import static org.folio.tools.kong.model.operator.IntOperator.LESS_THAN_OR_EQUAL;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.folio.tools.kong.model.operator.IntOperator;
import org.folio.tools.kong.model.operator.RouteOperator;

@Data
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class IntExpressionBuilder {

  static final String NET_PORT_FIELD = "net.port";
  static final String NET_SRC_PORT_FIELD = "net.src.port";
  static final String NET_DST_PORT_FIELD = "net.dst.port";

  private String key;

  /**
   * Creates a {@link RouteExpression} for state when field must be equal to a value.
   *
   * @param value - int value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression equalsTo(int value) {
    return buildRouteExpression(key, IntOperator.EQUALS, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must not be equal to a value.
   *
   * @param value - int value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression notEqualsTo(int value) {
    return buildRouteExpression(key, IntOperator.NOT_EQUALS, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must be greater than a value.
   *
   * @param value - int value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression greaterThan(int value) {
    return buildRouteExpression(key, GREATER_THAN, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must be greater or equal to a value.
   *
   * @param value - int value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression greaterThanOrEqualsTo(int value) {
    return buildRouteExpression(key, GREATER_THAN_OR_EQUAL, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must be less than a value.
   *
   * @param value - int value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression lessThan(int value) {
    return buildRouteExpression(key, LESS_THAN, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must be less or equal to a value.
   *
   * @param value - int value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression lessThanOrEqualsTo(int value) {
    return buildRouteExpression(key, LESS_THAN_OR_EQUAL, value);
  }

  private static RouteExpression buildRouteExpression(String key, RouteOperator operator, int value) {
    return new RouteExpression(key, operator, String.valueOf(value));
  }
}
