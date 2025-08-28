package org.folio.security.integration.keycloak.utils;

import static org.folio.security.integration.keycloak.utils.KeycloakSecretUtils.GLOBAL_SECTION;
import static org.folio.security.integration.keycloak.utils.KeycloakSecretUtils.globalStoreKey;
import static org.folio.security.integration.keycloak.utils.KeycloakSecretUtils.tenantStoreKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

import org.folio.common.configuration.properties.FolioEnvironment;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.MockedStatic;

@UnitTest
class KeycloakSecretUtilsTest {

  private static final String TEST_ENV = "test";
  private static final String TEST_CLIENT_ID = "test-client-id";
  private static final String TEST_TENANT = "test-tenant";

  @Test
  void globalStoreKey_positive() {
    try (MockedStatic<FolioEnvironment> mockedEnv = mockStatic(FolioEnvironment.class)) {
      mockedEnv.when(FolioEnvironment::getFolioEnvName).thenReturn(TEST_ENV);

      var key = globalStoreKey(TEST_CLIENT_ID);

      assertEquals(key(GLOBAL_SECTION, TEST_CLIENT_ID), key);
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  void globalStoreKey_negative_clientIdEmpty(String clientId) {
    var exc = assertThrows(IllegalArgumentException.class, () -> globalStoreKey(clientId));

    assertEquals(exc.getMessage(), "Client id cannot be empty");
  }

  @Test
  void tenantStoreKey_positive() {
    try (MockedStatic<FolioEnvironment> mockedEnv = mockStatic(FolioEnvironment.class)) {
      mockedEnv.when(FolioEnvironment::getFolioEnvName).thenReturn(TEST_ENV);

      var key = tenantStoreKey(TEST_TENANT, TEST_CLIENT_ID);

      assertEquals(key(TEST_TENANT, TEST_CLIENT_ID), key);
    }
  }

  @ParameterizedTest
  @NullAndEmptySource
  void tenantStoreKey_negative_clientIdEmpty(String clientId) {
    var exc = assertThrows(IllegalArgumentException.class, () -> tenantStoreKey(TEST_TENANT, clientId));

    assertEquals(exc.getMessage(), "Client id cannot be empty");
  }

  @ParameterizedTest
  @NullAndEmptySource
  void tenantStoreKey_negative_tenantEmpty(String tenant) {
    var exc = assertThrows(IllegalArgumentException.class, () -> tenantStoreKey(tenant, TEST_CLIENT_ID));

    assertEquals(exc.getMessage(), "Tenant cannot be empty");
  }

  private static String key(String tenant, String client) {
    return String.format("%s_%s_%s", TEST_ENV, tenant, client);
  }
}
