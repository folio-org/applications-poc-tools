package org.folio.common.utils.permission.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionAction {

  VIEW("view"),
  CREATE("create"),
  EDIT("edit"),
  DELETE("delete"),
  MANAGE("manage"),
  EXECUTE("execute");

  private final String value;

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static PermissionAction fromValue(String value) {
    for (PermissionAction b : PermissionAction.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
