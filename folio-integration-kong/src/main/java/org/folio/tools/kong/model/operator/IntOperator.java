package org.folio.tools.kong.model.operator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IntOperator implements RouteOperator {

  EQUALS("=="),
  NOT_EQUALS("!="),
  GREATER_THAN(">"),
  GREATER_THAN_OR_EQUAL(">="),
  LESS_THAN("<"),
  LESS_THAN_OR_EQUAL("<=");

  private final String stringValue;
}
