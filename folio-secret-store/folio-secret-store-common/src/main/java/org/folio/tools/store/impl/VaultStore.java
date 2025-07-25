package org.folio.tools.store.impl;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.folio.tools.store.impl.Validation.validateKey;
import static org.folio.tools.store.impl.Validation.validateValue;
import static org.folio.tools.store.properties.VaultConfigProperties.DEFAULT_VAULT_SECRET_ROOT;

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.folio.tools.store.exception.SecureStoreServiceException;
import org.folio.tools.store.properties.VaultConfigProperties;

@Log4j2
public final class VaultStore implements SecureStore {

  public static final String TYPE = "Vault";
  public static final String PROP_VAULT_TOKEN = "token";
  public static final String PROP_VAULT_ADDRESS = "address";
  public static final String PROP_VAULT_SECRET_ROOT = "secretRoot";
  public static final String PROP_VAULT_USE_SSL = "enableSSL";
  public static final String PROP_SSL_PEM_FILE = "ssl.pem.path";
  public static final String PROP_TRUSTSTORE_JKS_FILE = "ssl.truststore.jks.path";
  public static final String PROP_KEYSTORE_JKS_FILE = "ssl.keystore.jks.path";
  public static final String PROP_KEYSTORE_PASS = "ssl.keystore.password";
  public static final String DEFAULT_VAULT_ADDRESS = "http://127.0.0.1:8200";
  public static final String DEFAULT_VAULT_USER_SSL = "false";

  private Vault vault;
  private String secretRoot;

  public VaultStore(Properties properties) {
    log.info("Initializing...");
    requireNonNull(properties, "Properties cannot be null");
    var vaultConfigProperties = mapPropertiesToVaultConfigProperties(properties);
    try {
      this.vault = createVault(vaultConfigProperties);
      this.secretRoot = vaultConfigProperties.getSecretRoot();
    } catch (Exception e) {
      log.error("Failed to initialize: ", e);
      throw new IllegalStateException(format("Cannot initialize vault: message = %s", e.getMessage()), e);
    }
  }

  private VaultStore(VaultConfigProperties properties) {
    try {
      this.vault = createVault(properties);
      this.secretRoot = properties.getSecretRoot();
    } catch (Exception e) {
      log.error("Failed to initialize: ", e);
      throw new IllegalStateException(format("Cannot initialize vault: message = %s", e.getMessage()), e);
    }
  }

  public static VaultStore create(VaultConfigProperties properties) {
    return new VaultStore(properties);
  }

  @Override
  public String get(String key) {
    log.debug("Getting value for key: {}", key);

    var vaultKey = VaultKey.from(key);
    return getValue(vaultKey);
  }

  @Override
  public void set(String key, String value) {
    log.debug("Setting value for key: {}", key);

    var vaultKey = VaultKey.from(key);
    validateValue(value);

    setValue(vaultKey, value);
  }

  @Override
  public void delete(String key) {
    log.debug("Removing value for key: {}", key);

    var vaultKey = VaultKey.from(key);
    deleteValue(vaultKey);
  }

  private String getValue(VaultKey vaultKey) {
    var path = vaultKey.getPath();
    var secretName = vaultKey.getSecretName();

    log.debug("Retrieving secret for: path = {}, secret name = {}", path, secretName);
    try {
      var secretPath = addRootPath(path);

      var ret = vault.logical()
        .read(secretPath)
        .getData()
        .get(secretName);
      if (ret == null) {
        throw new SecretNotFoundException(format("Attribute: %s not set for %s", secretName, path));
      }
      return ret;
    } catch (VaultException e) {
      throw new SecureStoreServiceException("Failed to get secret: key = " + vaultKey
        + ", error = " + e.getMessage(), e);
    }
  }

  private void setValue(VaultKey vaultKey, String value) {
    var path = vaultKey.getPath();
    var secretName = vaultKey.getSecretName();

    log.debug("Setting secret for: path = {}, secret name = {}", path, secretName);
    try {
      var secretPath = addRootPath(path);
      mergeSecrets(secretPath, secretName, value);
    } catch (VaultException e) {
      throw new SecureStoreServiceException("Failed to save secret: key = " + vaultKey
        + ", error = " + e.getMessage(), e);
    }
  }

  private void deleteValue(VaultKey vaultKey) {
    var path = vaultKey.getPath();
    var secretName = vaultKey.getSecretName();

    log.debug("Deleting secret for: path = {}, secret name = {}", path, secretName);
    try {
      var secretPath = addRootPath(path);
      removeSecret(secretPath, secretName);
    } catch (VaultException e) {
      throw new SecureStoreServiceException("Failed to delete secret: key = " + vaultKey
        + ", error = " + e.getMessage(), e);
    }
  }

  private String addRootPath(String path) {
    return isNotEmpty(secretRoot)
      ? secretRoot + "/" + path
      : path;
  }

  private void mergeSecrets(String secretPath, String secretName, String value) throws VaultException {
    var existingSecrets = vault.logical().read(secretPath).getData();
    if (Objects.equals(existingSecrets.get(secretName), value)) {
      return; // No need to update if the value is the same
    }
    
    var updatedSecrets = new HashMap<String, Object>(existingSecrets);
    updatedSecrets.put(secretName, value);
    vault.logical().write(secretPath, updatedSecrets);
  }

  private void removeSecret(String secretPath, String secretName) throws VaultException {
    var existingSecrets = vault.logical().read(secretPath).getData();
    if (!existingSecrets.containsKey(secretName)) {
      return; // Nothing to remove
    }

    var updatedSecrets = new HashMap<String, Object>(existingSecrets);
    updatedSecrets.remove(secretName);
    vault.logical().write(secretPath, updatedSecrets);
  }

  private static Vault createVault(VaultConfigProperties vaultConfigProperties) throws VaultException {
    var config = new VaultConfig()
      .address(vaultConfigProperties.getAddress())
      .token(vaultConfigProperties.getToken());
    if (nonNull(vaultConfigProperties.getEnableSsl()) && vaultConfigProperties.getEnableSsl()) {
      var sslConfig = createSslConfig(vaultConfigProperties);
      config.sslConfig(sslConfig);
    }
    return new Vault(config.build());
  }

  private static SslConfig createSslConfig(VaultConfigProperties properties) throws VaultException {
    var sslConfig = new SslConfig();
    if (nonNull(properties.getPemFilePath())) {
      sslConfig.clientKeyPemFile(new File(properties.getPemFilePath()));
    }
    if (nonNull(properties.getTruststoreFilePath())) {
      sslConfig.trustStoreFile(new File(properties.getTruststoreFilePath()));
    }
    if (nonNull(properties.getKeystoreFilePath())) {
      sslConfig.keyStoreFile(new File(properties.getKeystoreFilePath()), properties.getKeystorePassword());
    }
    return sslConfig;
  }

  private static VaultConfigProperties mapPropertiesToVaultConfigProperties(Properties properties) {
    return VaultConfigProperties.builder()
      .address(properties.getProperty(PROP_VAULT_ADDRESS, DEFAULT_VAULT_ADDRESS))
      .token(properties.getProperty(PROP_VAULT_TOKEN))
      .secretRoot(properties.getProperty(PROP_VAULT_SECRET_ROOT, DEFAULT_VAULT_SECRET_ROOT))
      .enableSsl(Boolean.parseBoolean(properties.getProperty(PROP_VAULT_USE_SSL, DEFAULT_VAULT_USER_SSL)))
      .pemFilePath(properties.getProperty(PROP_SSL_PEM_FILE))
      .truststoreFilePath(properties.getProperty(PROP_TRUSTSTORE_JKS_FILE))
      .keystorePassword(properties.getProperty(PROP_KEYSTORE_PASS))
      .keystoreFilePath(properties.getProperty(PROP_KEYSTORE_JKS_FILE))
      .build();
  }

  @Value
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  private static final class VaultKey {

    String path;
    String secretName;

    static VaultKey from(String key) {
      var keyParts = getKeyParts(key);

      var path = getKeyPath(keyParts);
      var secretName = keyParts[keyParts.length - 1];

      return new VaultKey(path, secretName);
    }

    private static String[] getKeyParts(String key) {
      validateKey(key);

      var keyParts = key.split("_");
      if (keyParts.length < 2) {
        throw new IllegalArgumentException("Key should consist of at least two parts separated by '_'");
      }
      return keyParts;
    }

    private static String getKeyPath(String[] keyParts) {
      return String.join("/", Arrays.copyOf(keyParts, keyParts.length - 1));
    }

    @Override
    public String toString() {
      return format("%s/%s", path, secretName);
    }
  }
}
