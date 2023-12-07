package org.folio.common.domain.model.error;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ErrorCode {
  UNKNOWN_ERROR("unknown_error"),
  SERVICE_ERROR("service_error"),
  VALIDATION_ERROR("validation_error"),
  NOT_FOUND_ERROR("not_found_error"),
  FOUND_ERROR("found_error"),
  ROUTE_NOT_FOUND_ERROR("route_not_found_error"),
  AUTH_ERROR("auth_error");

  private String value;

  ErrorCode(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static ErrorCode fromValue(String value) {
    for (ErrorCode b : ErrorCode.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

