package org.folio.tools.kong.model.expression;

import static java.util.Objects.requireNonNull;
import static org.folio.tools.kong.model.operator.StringOperator.CONTAINS;
import static org.folio.tools.kong.model.operator.StringOperator.EQUALS;
import static org.folio.tools.kong.model.operator.StringOperator.NOT_EQUALS;
import static org.folio.tools.kong.model.operator.StringOperator.PREFIX_MATCHING;
import static org.folio.tools.kong.model.operator.StringOperator.REGEX_MATCHING;
import static org.folio.tools.kong.model.operator.StringOperator.SUFFIX_MATCHING;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.folio.tools.kong.model.operator.RouteOperator;
import org.folio.tools.kong.model.transformation.StringTransformations;

@Data
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class StringExpressionBuilder {

  static final String NET_PROTOCOL_FIELD = "net.protocol";
  static final String TLS_SNI_FIELD = "tls.sni";
  static final String HTTP_METHOD_FIELD = "http.method";
  static final String HTTP_PATH_FIELD = "http.path";
  static final String HTTP_HEADERS_FIELD_PREFIX = "http.headers.";
  static final String HTTP_QUERIES_FIELD_PREFIX = "http.queries.";

  private String field;

  /**
   * Applies transformation wrapper to the key.
   *
   * @param transformation - {@link StringTransformations} enum value
   * @return {@link StringExpressionBuilder} object to finish route expression
   */
  public StringExpressionBuilder withTransformation(StringTransformations transformation) {
    this.field = transformation.getKeyModifier().apply(field);
    return this;
  }

  /**
   * Creates a {@link RouteExpression} for state when field must be equal to a value.
   *
   * @param value - string value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression equalsTo(String value) {
    return buildRouteExpression(field, EQUALS, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must not be equal to a value.
   *
   * @param value - string value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression notEqualsTo(String value) {
    return buildRouteExpression(field, NOT_EQUALS, value);
  }

  /**
   * Creates a {@link RouteExpression} for state field must have prefix matching a a value.
   *
   * @param value - string value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression prefixMatching(String value) {
    return buildRouteExpression(field, PREFIX_MATCHING, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must have suffix matching a value.
   *
   * @param value - string value
   * @return created {@link RouteExpression} object
   */
  public RouteExpression suffixMatching(String value) {
    return buildRouteExpression(field, SUFFIX_MATCHING, value);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must match a regex.
   *
   * @param regex - regular expression
   * @return created {@link RouteExpression} object
   */
  public RouteExpression regexMatching(String regex) {
    requireNonNull(regex, "StringExpression regex must not be null");
    return buildRouteExpression(field, REGEX_MATCHING, regex);
  }

  public RouteExpression headerRegexMatching(String regex) {
    requireNonNull(regex, "StringExpression regex must not be null");
    return buildRouteExpressionWithRegex(field, regex);
  }

  /**
   * Creates a {@link RouteExpression} for state when field must contain a value.
   *
   * @param value - regular expression
   * @return created {@link RouteExpression} object
   */
  public RouteExpression contains(String value) {
    return buildRouteExpression(field, CONTAINS, value);
  }

  private static RouteExpression buildRouteExpression(String key, RouteOperator operator, String value) {
    requireNonNull(value, "StringExpression value must not be null");
    return new RouteExpression(key, operator, "\"" + value + "\"");
  }

  private static RouteExpression buildRouteExpressionWithRegex(String key, String regex) {
    requireNonNull(regex, "StringExpression regex must not be null");
    return new RouteExpression(key, REGEX_MATCHING, "r#" + regex + "#");
  }
}
