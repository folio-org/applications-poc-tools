package org.folio.security.integration.keycloak.service;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.RequiredArgsConstructor;
import org.folio.tools.store.properties.SecureStoreProperties;

@RequiredArgsConstructor
public class SecureStoreKeyProvider {

  public static final String GLOBAL_SECTION = "master";

  private final SecureStoreProperties secureStoreProperties;

  public String globalStoreKey(String clientId) {
    return tenantStoreKey(GLOBAL_SECTION, clientId);
  }

  public String tenantStoreKey(String tenant, String clientId) {
    if (isBlank(tenant)) {
      throw new IllegalArgumentException("Tenant cannot be empty");
    }
    if (isBlank(clientId)) {
      throw new IllegalArgumentException("Client id cannot be empty");
    }

    return String.format("%s_%s_%s", secureStoreProperties.getEnvironment(), tenant, clientId);
  }
}
