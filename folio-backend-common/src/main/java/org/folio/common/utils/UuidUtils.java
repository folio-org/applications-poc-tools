package org.folio.common.utils;

import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UuidUtils {

  public static String randomId() {
    return UUID.randomUUID().toString();
  }
}
