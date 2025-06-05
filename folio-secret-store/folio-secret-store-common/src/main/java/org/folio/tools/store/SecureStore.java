package org.folio.tools.store;

import java.util.Optional;
import org.folio.tools.store.exception.SecretNotFoundException;

public interface SecureStore {

  /**
   * Retrieves the value associated with the specified key.
   *
   * @param key the key to look up
   * @return the value associated with the key
   * @throws SecretNotFoundException if no value is found for the given key
   */
  String get(String key);

  /**
   * Sets the value for the specified key. The value should not be null.
   * To remove a key, use {@link #delete(String)}.
   *
   * @param key the key to set
   * @param value the value to associate with the key
   */
  void set(String key, String value);

  /**
   * Deletes the value associated with the specified key. This operation is idempotent:
   * if the key does not exist, it will simply do nothing.
   *
   * @param key the key to delete
   */
  void delete(String key);

  /**
   * Looks up the value associated with the specified key, returning an Optional.
   * If the key does not exist, an empty Optional is returned.
   *
   * @param key the key to look up
   * @return an Optional containing the value if found, or empty if not found
   */
  default Optional<String> lookup(String key) {
    try {
      return Optional.ofNullable(get(key));
    } catch (SecretNotFoundException e) {
      return Optional.empty();
    }
  }
}
