package org.folio.tools.kong.model.expression;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.folio.tools.kong.model.expression.IntExpressionBuilder.NET_DST_PORT_FIELD;
import static org.folio.tools.kong.model.expression.IntExpressionBuilder.NET_PORT_FIELD;
import static org.folio.tools.kong.model.expression.IntExpressionBuilder.NET_SRC_PORT_FIELD;
import static org.folio.tools.kong.model.expression.IpAddrExpressionBuilder.NET_DST_IP_FIELD;
import static org.folio.tools.kong.model.expression.IpAddrExpressionBuilder.NET_SRC_IP_FIELD;
import static org.folio.tools.kong.model.expression.StringExpressionBuilder.HTTP_HEADERS_FIELD_PREFIX;
import static org.folio.tools.kong.model.expression.StringExpressionBuilder.HTTP_METHOD_FIELD;
import static org.folio.tools.kong.model.expression.StringExpressionBuilder.HTTP_PATH_FIELD;
import static org.folio.tools.kong.model.expression.StringExpressionBuilder.HTTP_QUERIES_FIELD_PREFIX;
import static org.folio.tools.kong.model.expression.StringExpressionBuilder.NET_PROTOCOL_FIELD;
import static org.folio.tools.kong.model.expression.StringExpressionBuilder.TLS_SNI_FIELD;
import static org.folio.tools.kong.model.operator.BoolOperator.AND;
import static org.folio.tools.kong.model.operator.BoolOperator.OR;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.folio.tools.kong.model.operator.BoolOperator;

public interface RouteExpressions {

  /**
   * The protocol used to communicate with the downstream application.
   *
   * @return {@link StringExpressionBuilder} to finish route expression
   */
  static StringExpressionBuilder netProtocol() {
    return new StringExpressionBuilder(NET_PROTOCOL_FIELD);
  }

  /**
   * Server name indication.
   *
   * @return {@link StringExpressionBuilder} to finish route expression
   */
  static StringExpressionBuilder tlsSni() {
    return new StringExpressionBuilder(TLS_SNI_FIELD);
  }

  /**
   * HTTP methods that match a route.
   *
   * @return {@link StringExpressionBuilder} to finish route expression
   */
  static StringExpressionBuilder httpMethod() {
    return new StringExpressionBuilder(HTTP_METHOD_FIELD);
  }

  /**
   * HTTP methods that match a route.
   *
   * @return {@link StringExpressionBuilder} to finish route expression
   */
  static StringExpressionBuilder httpPath() {
    return new StringExpressionBuilder(HTTP_PATH_FIELD);
  }

  /**
   * The protocol used to communicate with the downstream application.
   *
   * @param header - http header name as {@link String}
   * @return {@link StringExpressionBuilder} to finish route expression
   */
  static StringExpressionBuilder httpHeader(String header) {
    if (StringUtils.isEmpty(header) || StringUtils.containsAny(header, " ")) {
      throw new IllegalArgumentException(
        "Invalid header value, must not be blank and must not contain any whitespaces: " + header);
    }

    return new StringExpressionBuilder(HTTP_HEADERS_FIELD_PREFIX + replace(header, "-", "_").toLowerCase());
  }

  /**
   * The protocol used to communicate with the downstream application.
   *
   * @param parameter - query parameter name as {@link String}
   * @return {@link StringExpressionBuilder} to finish route expression
   */
  static StringExpressionBuilder queryParameter(String parameter) {
    requireNonNull(parameter, "Query parameter in stringExpression must not be null");
    return new StringExpressionBuilder(HTTP_QUERIES_FIELD_PREFIX + parameter);
  }

  /**
   * Server end port number.
   *
   * @return {@link IntExpressionBuilder} to finish route expression
   */
  static IntExpressionBuilder netPort() {
    return new IntExpressionBuilder(NET_PORT_FIELD);
  }

  /**
   * Source port number of incoming connection.
   *
   * @return {@link IntExpressionBuilder} to finish route expression
   */
  static IntExpressionBuilder netSrcPort() {
    return new IntExpressionBuilder(NET_SRC_PORT_FIELD);
  }

  /**
   * Destination port number of incoming connection.
   *
   * @return {@link IntExpressionBuilder} to finish route expression
   */
  static IntExpressionBuilder netDstPort() {
    return new IntExpressionBuilder(NET_DST_PORT_FIELD);
  }

  /**
   * Source IP address of incoming connection.
   *
   * @return {@link IpAddrExpressionBuilder} to finish route expression
   */
  static IpAddrExpressionBuilder netSrcIp() {
    return new IpAddrExpressionBuilder(NET_SRC_IP_FIELD);
  }

  /**
   * Destination IP address of incoming connection.
   *
   * @return {@link IpAddrExpressionBuilder} to finish route expression
   */
  static IpAddrExpressionBuilder netDstIp() {
    return new IpAddrExpressionBuilder(NET_DST_IP_FIELD);
  }

  /**
   * Creates a {@link BoolExpressionBuilder} from left {@link RouteExpressions} value.
   *
   * @param left - left route expression
   * @return {@link BoolExpressionBuilder} to finish route expression
   */
  static BoolExpressionBuilder bool(RouteExpression left) {
    return new BoolExpressionBuilder(left);
  }

  /**
   * Creates a boolean expression for multiple expressions using {@link BoolOperator#OR} operator.
   *
   * @param expressions - route expressions as array
   * @return created {@link RouteExpressions} object
   */
  static RouteExpression combineUsingOr(RouteExpression... expressions) {
    return new RouteExpression(combineExpressions(OR, Arrays.asList(expressions)));
  }

  /**
   * Creates a boolean expression for multiple expressions using {@link BoolOperator#OR} operator.
   *
   * @param expressions - route expressions as {@link List}
   * @return created {@link RouteExpressions} object
   */
  static RouteExpression combineUsingOr(List<RouteExpression> expressions) {
    return new RouteExpression(combineExpressions(OR, expressions));
  }

  /**
   * Creates a boolean expression for multiple expressions using {@link BoolOperator#AND} operator.
   *
   * @param expressions - route expressions as array
   * @return created {@link RouteExpressions} object
   */
  static RouteExpression combineUsingAnd(RouteExpression... expressions) {
    return new RouteExpression(combineExpressions(AND, Arrays.asList(expressions)));
  }

  /**
   * Creates a boolean expression for multiple expressions using {@link BoolOperator#AND} operator.
   *
   * @param expressions - route expressions as array
   * @return created {@link RouteExpressions} object
   */
  static RouteExpression combineUsingAnd(List<RouteExpression> expressions) {
    return new RouteExpression(combineExpressions(AND, expressions));
  }

  private static String combineExpressions(BoolOperator operator, List<RouteExpression> expressions) {
    if (expressions.size() == 1) {
      return expressions.get(0).toString();
    }

    var delimiter = " " + operator.getStringValue() + " ";
    return expressions.stream()
      .filter(Objects::nonNull)
      .map(RouteExpression::toString)
      .collect(Collectors.joining(delimiter, "(", ")"));
  }
}
