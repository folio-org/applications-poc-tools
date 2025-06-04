package org.folio.tools.store.impl;

import static org.folio.tools.store.impl.EphemeralStore.PROP_TENANTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Properties;
import org.folio.test.types.UnitTest;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.folio.tools.store.properties.EphemeralConfigProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class EphemeralStoreTest {

  public static final EphemeralConfigProperties CONFIG =
    EphemeralConfigProperties.builder().content(Map.of("foo", "bar")).build();

  @InjectMocks
  private EphemeralStore ephemeralStore = EphemeralStore.create(CONFIG);

  @Test
  void get_positive() {
    var key = "foo";
    var value = "bar";

    var result = ephemeralStore.get(key);

    assertEquals(value, result);
  }

  @Test
  void get_negative_notFound() {
    var key = "baz";

    assertThrows(SecretNotFoundException.class, () -> ephemeralStore.get(key));
  }

  @Test
  void lookup_positive() {
    var key = "foo";
    var value = "bar";

    var result = ephemeralStore.lookup(key);

    assertTrue(result.isPresent());
    assertEquals(value, result.get());
  }

  @Test
  void lookup_negative_notFound() {
    var key = "baz";

    var result = ephemeralStore.lookup(key);

    assertTrue(result.isEmpty());
  }

  @Test
  void set_positive() {
    var key = "foo";
    var value = "bar";

    ephemeralStore.set(key, value);

    assertEquals(value, ephemeralStore.get(key));
  }

  @Test
  void set_positive_nullValueDeletesKey() {
    var key = "foo";
    var value = "bar";
    ephemeralStore.set(key, value);
    assertEquals(value, ephemeralStore.get(key));

    ephemeralStore.set(key, null);
    // After setting null, the key should be deleted
    assertThrows(SecretNotFoundException.class, () -> ephemeralStore.get(key));
  }

  @Test
  void set_negative() {
    var key = "foo";
    var value = "bar";

    ephemeralStore.set(key, value);

    assertThrows(SecretNotFoundException.class, () -> ephemeralStore.get("baz"));
  }

  @Test
  void createEphemeralStoreViaProperties_positive() {
    var props = new Properties();
    props.setProperty(PROP_TENANTS, "dit ,dat, dot, done , empty");
    props.setProperty("dit", "dit,dit_password");
    props.setProperty("dat", "dat,dat_password");
    props.setProperty("dot", "dot,dot_password");
    props.setProperty("done", "done");

    var store = new EphemeralStore(props);

    assertEquals("dit_password", store.get("dit_dit"));
    assertEquals("dot_password", store.get("dot_dot"));
    assertEquals("dat_password", store.get("dat_dat"));
    assertEquals("", store.get("done_done"));
  }

  @Test
  void createEphemeralStoreViaProperties_negative() {
    var props = new Properties();
    props.setProperty(PROP_TENANTS, "dit ,dat, dot,done, empty ");
    props.setProperty("dit", "dit,dit_password");
    props.setProperty("dat", "dat,dat_password");
    props.setProperty("dot", "dot,dot_password");
    props.setProperty("done", "done");

    var store = new EphemeralStore(props);

    assertThrows(SecretNotFoundException.class, () -> store.get("dit_foo"));
    assertThrows(SecretNotFoundException.class, () -> store.get("dot_foo"));
    assertThrows(SecretNotFoundException.class, () -> store.get("dat_foo"));
    assertThrows(SecretNotFoundException.class, () -> store.get("done_foo"));
  }

  @Test
  void delete_positive() {
    var key = "foo";
    var value = "bar";
    ephemeralStore.set(key, value);

    ephemeralStore.delete(key);

    assertThrows(SecretNotFoundException.class, () -> ephemeralStore.get(key));
  }

  @Test
  void delete_negative_keyNotPresent() {
    var key = "not_existing_key";
    // Should not throw any exception
    ephemeralStore.delete(key);
    // Still not present
    assertThrows(SecretNotFoundException.class, () -> ephemeralStore.get(key));
  }

  @Test
  void delete_negative_invalidKey() {
    assertThrows(IllegalArgumentException.class, () -> ephemeralStore.delete(""));
    assertThrows(IllegalArgumentException.class, () -> ephemeralStore.delete(null));
  }
}
