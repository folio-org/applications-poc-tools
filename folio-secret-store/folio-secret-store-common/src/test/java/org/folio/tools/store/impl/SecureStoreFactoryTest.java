package org.folio.tools.store.impl;

import static org.folio.tools.store.properties.FsspConfigProperties.PROP_FSSP_TRUSTSTORE_FILE_TYPE;
import static org.folio.tools.store.properties.FsspConfigProperties.PROP_FSSP_TRUSTSTORE_PASSWORD;
import static org.folio.tools.store.properties.FsspConfigProperties.PROP_FSSP_TRUSTSTORE_PATH;
import static org.folio.tools.store.support.SecretStoreTestValues.FSSP_CLIENT_TRUSTSTORE_PATH;
import static org.folio.tools.store.support.SecretStoreTestValues.KS_FILE_TYPE_PKCS12;
import static org.folio.tools.store.support.SecretStoreTestValues.KS_PASSWORD;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Properties;
import org.folio.test.types.UnitTest;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.SecureStoreFactory;
import org.junit.jupiter.api.Test;

@UnitTest
class SecureStoreFactoryTest {

  public static final Class<? extends SecureStore> DEFAULT_SS_CLASS = EphemeralStore.class;

  @Test
  void getSecureStoreKnownTypes()
    throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
    Class<?>[] stores = new Class<?>[] {AwsStore.class, EphemeralStore.class, VaultStore.class, FsspStore.class};

    SecureStore actual;

    for (Class<?> clazz : stores) {
      Properties props = new Properties();

      if (clazz.equals(AwsStore.class)) {
        props.put(AwsStore.PROP_REGION, "us-east-1");
      } else if (clazz.equals(FsspStore.class)) {
        props.put(PROP_FSSP_TRUSTSTORE_PATH, FSSP_CLIENT_TRUSTSTORE_PATH);
        props.put(PROP_FSSP_TRUSTSTORE_FILE_TYPE, KS_FILE_TYPE_PKCS12);
        props.put(PROP_FSSP_TRUSTSTORE_PASSWORD, KS_PASSWORD);
      }

      actual = SecureStoreFactory.getSecureStore((String) clazz.getField("TYPE").get(null), props);
      assertThat(actual, instanceOf(clazz));

      try {
        actual = SecureStoreFactory.getSecureStore((String) clazz.getField("TYPE").get(null), null);
        assertThat(actual, instanceOf(clazz));
      } catch (Throwable t) {
        if (clazz.equals(VaultStore.class)) {
          assertThat(t.getClass(), equalTo(NullPointerException.class));
        } else if (clazz.equals(AwsStore.class)) {
          assertThat(t.getClass(), equalTo(NullPointerException.class));
        } else if (clazz.equals(FsspStore.class)) {
          assertThat(t.getClass(), equalTo(NullPointerException.class));
        } else {
          fail(String.format("Unexpected Exception thrown for class: %s %s", clazz.getName(), t.getMessage()));
        }
      }
    }
  }

  @Test
  void getSecureStoreDefaultType() {
    SecureStore actual;

    actual = SecureStoreFactory.getSecureStore("foo", new Properties());
    assertThat(actual, instanceOf(DEFAULT_SS_CLASS));
    actual = SecureStoreFactory.getSecureStore("foo", null);
    assertThat(actual, instanceOf(DEFAULT_SS_CLASS));

    actual = SecureStoreFactory.getSecureStore(null, new Properties());
    assertThat(actual, instanceOf(DEFAULT_SS_CLASS));
    actual = SecureStoreFactory.getSecureStore(null, null);
    assertThat(actual, instanceOf(DEFAULT_SS_CLASS));
  }
}
