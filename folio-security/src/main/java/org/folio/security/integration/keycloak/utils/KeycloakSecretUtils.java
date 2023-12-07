package org.folio.security.integration.keycloak.utils;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.folio.common.configuration.properties.FolioEnvironment.getFolioEnvName;

import lombok.experimental.UtilityClass;

@UtilityClass
public class KeycloakSecretUtils {

  public static final String GLOBAL_SECTION = "master";

  public static String globalStoreKey(String clientId) {
    return tenantStoreKey(GLOBAL_SECTION, clientId);
  }

  public static String tenantStoreKey(String tenant, String clientId) {
    if (isBlank(tenant)) {
      throw new IllegalArgumentException("Tenant cannot be empty");
    }
    if (isBlank(clientId)) {
      throw new IllegalArgumentException("Client id cannot be empty");
    }

    return String.format("%s_%s_%s", getFolioEnvName(), tenant, clientId);
  }
}
