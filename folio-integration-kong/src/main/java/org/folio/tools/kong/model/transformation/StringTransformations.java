package org.folio.tools.kong.model.transformation;

import java.util.function.Function;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StringTransformations {

  LOWER("lower", key -> "lower(" + key + ")");

  /**
   * Transformation name.
   */
  private final String name;

  /**
   * Key modifier in {@link org.folio.tools.kong.model.expression.RouteExpression} object.
   */
  private final Function<String, String> keyModifier;
}
