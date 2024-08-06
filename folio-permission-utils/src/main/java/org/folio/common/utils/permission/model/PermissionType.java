package org.folio.common.utils.permission.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PermissionType {

  DATA("data"),
  SETTINGS("settings"),
  PROCEDURAL("procedural");

  private final String value;

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static PermissionType fromValue(String value) {
    for (PermissionType b : PermissionType.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
