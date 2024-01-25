package org.folio.tools.kong.model.operator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BoolOperator implements RouteOperator {

  AND("&&"),
  OR("||");

  private final String stringValue;
}
