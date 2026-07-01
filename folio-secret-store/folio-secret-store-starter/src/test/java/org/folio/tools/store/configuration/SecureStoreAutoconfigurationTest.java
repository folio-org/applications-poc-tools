package org.folio.tools.store.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.SecureStoreType;
import org.folio.tools.store.impl.AwsStore;
import org.folio.tools.store.impl.EphemeralStore;
import org.folio.tools.store.properties.AwsProperties;
import org.folio.tools.store.properties.EphemeralProperties;
import org.folio.tools.store.properties.FsspProperties;
import org.folio.tools.store.properties.SecureStoreProperties;
import org.folio.tools.store.properties.VaultProperties;
import org.junit.jupiter.api.Test;

/**
 * Verifies the D2 runtime backend selection: the single {@code secureStore} bean constructs only the backend
 * named by {@code application.secret-store.type}. Only the connection-free backends (EPHEMERAL, AWS_SSM — whose
 * SSM client is built lazily) are exercised here; VAULT/FSSP build clients in their constructors and are
 * covered by their own integration tests.
 */
@UnitTest
class SecureStoreAutoconfigurationTest {

  private final SecureStoreAutoconfiguration configuration = new SecureStoreAutoconfiguration();

  @Test
  void secureStore_positive_selectsEphemeralByDefault() {
    var properties = new SecureStoreProperties();

    var secureStore = selectSecureStore(properties);

    assertThat(secureStore).isInstanceOf(EphemeralStore.class);
  }

  @Test
  void secureStore_positive_selectsAwsStoreForAwsSsmType() {
    var properties = new SecureStoreProperties();
    properties.setType(SecureStoreType.AWS_SSM);

    var secureStore = selectSecureStore(properties);

    assertThat(secureStore).isInstanceOf(AwsStore.class);
  }

  private SecureStore selectSecureStore(SecureStoreProperties properties) {
    return configuration.secureStore(properties, new EphemeralProperties(), new AwsProperties(),
      new VaultProperties(), new FsspProperties());
  }
}
