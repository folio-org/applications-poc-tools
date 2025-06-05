package org.folio.tools.store.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.folio.tools.store.impl.Validation.validateKey;
import static software.amazon.awssdk.utils.CollectionUtils.isNotEmpty;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.folio.tools.store.properties.EphemeralConfigProperties;

@Log4j2
public final class EphemeralStore implements SecureStore {

  public static final String TYPE = "Ephemeral";
  public static final String PROP_TENANTS = "tenants";
  public static final Pattern COMMA = Pattern.compile("\\s*[,]\\s*");

  private final Map<String, String> store;

  public EphemeralStore(Properties properties) {
    log.info("Initializing...");

    store = new ConcurrentHashMap<>();
    populateStoreFromProperties(properties);

    if (store.isEmpty()) {
      log.warn("Attention: No credentials were found/loaded");
    }
  }

  private EphemeralStore(EphemeralConfigProperties properties) {
    if (nonNull(properties) && isNotEmpty(properties.getContent())) {
      store = new ConcurrentHashMap<>(properties.getContent());
    } else {
      store = new ConcurrentHashMap<>();
    }
  }

  @Override
  public String get(String key) {
    validateKey(key);

    var value = store.get(key);
    if (isNull(value)) {
      throw new SecretNotFoundException("Nothing associated w/ key: " + key);
    }
    return value;
  }

  @Override
  public void set(String key, String value) {
    validateKey(key);
    // allow null values for backward compatibility, otherwise delete method should be used to remove a key
    if (value == null) {
      delete(key);
    } else {
      store.put(key, value);
    }
  }

  @Override
  public void delete(String key) {
    validateKey(key);
    store.remove(key);
  }

  public static EphemeralStore create(EphemeralConfigProperties properties) {
    return new EphemeralStore(properties);
  }

  private void put(String tenant, String username, String value) {
    store.put(getKey(tenant, username), value);
  }

  private String getKey(String tenant, String username) {
    return String.format("%s_%s", tenant, username);
  }

  private void populateStoreFromProperties(Properties properties) {
    if (isNull(properties)) {
      return;
    }

    var data = properties.getProperty(PROP_TENANTS);
    if (isNull(data)) {
      return;
    }

    var tenantIds = COMMA.split(data);
    for (var tenantId : tenantIds) {
      var tenantData = properties.getProperty(tenantId);
      if (nonNull(tenantData)) {
        String[] credentials = COMMA.split(tenantData);
        String user = credentials[0];
        String password = credentials.length > 1 ? credentials[1] : Strings.EMPTY;
        put(tenantId, user, password);
      } else {
        log.error("Error extracting user/password for tenantId: {}", tenantId);
      }
    }
  }
}
