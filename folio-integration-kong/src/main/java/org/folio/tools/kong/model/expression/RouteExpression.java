package org.folio.tools.kong.model.expression;

import lombok.Data;
import lombok.Getter;
import org.folio.tools.kong.model.operator.RouteOperator;

@Data
@Getter
public class RouteExpression {

  private final String expression;

  /**
   * Default required arguments constructor.
   *
   * @param expression - kong route expression as {@link String}
   */
  public RouteExpression(String expression) {
    this.expression = expression;
  }

  /**
   * A constructor for 3 arguments: field name , operator and value.
   *
   * @param field - field name from Kong specification
   * @param operator - field operator
   * @param value - comparison value
   */
  public RouteExpression(String field, RouteOperator operator, String value) {
    this(field + " " + operator.getStringValue() + " " + value);
  }

  @Override
  public String toString() {
    return expression;
  }
}
