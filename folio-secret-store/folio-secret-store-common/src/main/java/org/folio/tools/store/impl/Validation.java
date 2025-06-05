package org.folio.tools.store.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Validation {

  public static void validateKey(String key) {
    if (isBlank(key)) {
      throw new IllegalArgumentException("Key cannot be blank");
    }
  }

  public static void validateValue(String value) {
    if (isBlank(value)) {
      throw new IllegalArgumentException("Value cannot be blank");
    }
  }
}
