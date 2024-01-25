package org.folio.tools.kong.model.operator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StringOperator implements RouteOperator {

  EQUALS("=="),
  NOT_EQUALS("!="),
  REGEX_MATCHING("~"),
  PREFIX_MATCHING("^="),
  SUFFIX_MATCHING("=^"),
  CONTAINS("contains");

  private final String stringValue;
}
