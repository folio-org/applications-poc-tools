package org.folio.security.integration.keycloak.service;

import static org.folio.security.integration.keycloak.service.SecureStoreKeyProvider.GLOBAL_SECTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.folio.test.types.UnitTest;
import org.folio.tools.store.properties.SecureStoreProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

@UnitTest
class SecureStoreKeyProviderTest {

  private static final String TEST_ENV = "test";
  private static final String TEST_CLIENT_ID = "test-client-id";
  private static final String TEST_TENANT = "test-tenant";

  @Test
  void globalStoreKey_positive() {
    var key = globalStoreKey(TEST_CLIENT_ID);

    assertEquals(key(GLOBAL_SECTION, TEST_CLIENT_ID), key);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void globalStoreKey_negative_clientIdEmpty(String clientId) {
    var exc = assertThrows(IllegalArgumentException.class, () -> globalStoreKey(clientId));

    assertEquals("Client id cannot be empty", exc.getMessage());
  }

  @Test
  void tenantStoreKey_positive() {
    var key = tenantStoreKey(TEST_TENANT, TEST_CLIENT_ID);

    assertEquals(key(TEST_TENANT, TEST_CLIENT_ID), key);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void tenantStoreKey_negative_clientIdEmpty(String clientId) {
    var exc = assertThrows(IllegalArgumentException.class, () -> tenantStoreKey(TEST_TENANT, clientId));

    assertEquals("Client id cannot be empty", exc.getMessage());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void tenantStoreKey_negative_tenantEmpty(String tenant) {
    var exc = assertThrows(IllegalArgumentException.class, () -> tenantStoreKey(tenant, TEST_CLIENT_ID));

    assertEquals("Tenant cannot be empty", exc.getMessage());
  }

  private static String key(String tenant, String client) {
    return String.format("%s_%s_%s", TEST_ENV, tenant, client);
  }

  private static String globalStoreKey(String clientId) {
    var secureStoreProperties = mock(SecureStoreProperties.class);
    when(secureStoreProperties.getEnvironment()).thenReturn(TEST_ENV);
    var keycloakStoreKeyProvider = new SecureStoreKeyProvider(secureStoreProperties);
    return keycloakStoreKeyProvider.globalStoreKey(clientId);
  }

  private static String tenantStoreKey(String tenant, String clientId) {
    var secureStoreProperties = mock(SecureStoreProperties.class);
    when(secureStoreProperties.getEnvironment()).thenReturn(TEST_ENV);
    var keycloakStoreKeyProvider = new SecureStoreKeyProvider(secureStoreProperties);
    return keycloakStoreKeyProvider.tenantStoreKey(tenant, clientId);
  }
}
