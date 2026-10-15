package org.folio.common.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ApplicationScope {
  REGULAR("regular"),
  PLATFORM("platform");

  private final String value;

  ApplicationScope(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

  @JsonCreator
  public static ApplicationScope fromValue(String value) {
    for (var scope : ApplicationScope.values()) {
      if (scope.value.equals(value)) {
        return scope;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
