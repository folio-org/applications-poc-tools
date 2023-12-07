package org.folio.common.utils;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class KeycloakPermissionUtils {

  public static String toPermissionName(List<String> scopes, String policy, String resource) {
    return String.format("%s access for '%s' to '%s'", scopes, policy, resource);
  }
}
