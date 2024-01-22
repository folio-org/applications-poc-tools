package org.folio.tools.kong.model.operator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IpAddressOperator implements RouteOperator {

  EQUALS("=="),
  NOT_EQUALS("!="),
  IN("in"),
  NOT_IN("not in");

  private final String stringValue;
}
