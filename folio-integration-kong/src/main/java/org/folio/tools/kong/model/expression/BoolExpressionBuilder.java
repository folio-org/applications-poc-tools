package org.folio.tools.kong.model.expression;

import static java.util.Objects.requireNonNull;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.folio.tools.kong.model.operator.BoolOperator;

@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class BoolExpressionBuilder {

  private RouteExpression left;

  /**
   * Returns a {@link RouteExpression} combining left and right expressions using {@link BoolOperator#AND}.
   *
   * @param right - right {@link RouteExpression} object
   * @return created {@link RouteExpression} object
   */
  public RouteExpression and(RouteExpression right) {
    return buildRouteExpression(left, BoolOperator.AND, right);
  }

  /**
   * Returns a {@link RouteExpression} combining left and right expressions using {@link BoolOperator#OR}.
   *
   * @param right - right {@link RouteExpression} object
   * @return created {@link RouteExpression} object
   */
  public RouteExpression or(RouteExpression right) {
    return buildRouteExpression(left, BoolOperator.OR, right);
  }

  private static RouteExpression buildRouteExpression(RouteExpression left, BoolOperator op, RouteExpression right) {
    requireNonNull(left, "Left value of BooleanExpression must not be null");
    requireNonNull(right, "Right value of BooleanExpression must not be null");
    return new RouteExpression("(" + left + " " + op.getStringValue() + " " + right + ")");
  }
}
