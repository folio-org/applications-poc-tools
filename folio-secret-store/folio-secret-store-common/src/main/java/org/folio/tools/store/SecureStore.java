package org.folio.tools.store;

import java.util.Optional;

public interface SecureStore {

  /**
   * Returns secret for given client id, tenant and username.
   *
   * @param clientId - client id for which secret is stored
   * @param tenant - tenant id for which secret is stored
   * @param username - username for which secret is stored
   * @return secret as @String
   * @since 1.0.0
   * @deprecated use {@link #get(String)} instead.
   */
  @Deprecated
  String get(String clientId, String tenant, String username);

  String get(String key);

  void set(String key, String value);

  default Optional<String> lookup(String key) {
    try {
      return Optional.ofNullable(get(key));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
