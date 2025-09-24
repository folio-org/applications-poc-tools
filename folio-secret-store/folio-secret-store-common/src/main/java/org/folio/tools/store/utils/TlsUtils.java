package org.folio.tools.store.utils;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.SystemProperties.getJdkInternalHttpClientDisableHostNameVerification;
import static org.folio.tools.store.utils.ResourceUtils.getFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@Log4j2
@UtilityClass
public class TlsUtils {

  public static final boolean IS_HOSTNAME_VERIFICATION_DISABLED = // enabled by default
    hostnameVerificationDisabledValue();

  public static SSLContext buildSslContext(TlsProperties tls) {
    try {
      var trustManager = getTrustManager(tls);
      var keyManager = getKeyManager(tls);

      return sslContext(keyManager, trustManager);
    } catch (Exception e) {
      log.error("Error creating SSL context", e);
      throw new IllegalStateException("Error creating SSL context", e);
    }
  }

  private static X509TrustManager getTrustManager(TlsProperties tls)
    throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
    var ts = tls.getTrustStore();
    requireNonNull(ts.getPath(), "Trust store path is not defined");

    var tsKeyStore = initKeyStore(ts);
    return trustManager(tsKeyStore);
  }

  private static X509KeyManager getKeyManager(TlsProperties tls)
    throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
    var ks = tls.getKeyStore();

    if (ks != null && isNotBlank(ks.getPath())) {
      var keyStore = initKeyStore(ks);
      return keyManager(keyStore, ks.getPassword());
    } else {
      return null;
    }
  }

  private static KeyStore initKeyStore(TlsProperties.Store store)
    throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

    var keyStore = KeyStore.getInstance(
      isBlank(store.getType()) ? KeyStore.getDefaultType() : store.getType());

    try (var is = new FileInputStream(getFile(store.getPath()))) {
      keyStore.load(is, store.getPassword().toCharArray());
    }

    log.debug("Keystore initialized from file: keyStoreType = {}, file = {}", keyStore.getType(), store.getPath());
    return keyStore;
  }

  private static X509TrustManager trustManager(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException {
    var tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(keyStore);

    var trustManagers = tmf.getTrustManagers();
    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
      throw new IllegalStateException("Unexpected default trust managers: " + Arrays.toString(trustManagers));
    }
    return (X509TrustManager) trustManagers[0];
  }

  private static X509KeyManager keyManager(KeyStore keyStore, String password)
    throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException {

    var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, password.toCharArray());

    var keyManagers = kmf.getKeyManagers();
    if (keyManagers.length != 1 || !(keyManagers[0] instanceof X509KeyManager)) {
      throw new IllegalStateException("Unexpected default key managers: " + Arrays.toString(keyManagers));
    }
    return (X509KeyManager) keyManagers[0];
  }

  private static SSLContext sslContext(X509KeyManager keyManager, X509TrustManager trustManager)
    throws NoSuchAlgorithmException, KeyManagementException {

    var sslContext = SSLContext.getInstance("TLS");

    sslContext.init(
      keyManager != null ? new KeyManager[] {keyManager} : null,
      new TrustManager[] {trustManager},
      null);

    log.debug("SSL context initialized: protocol = {}", sslContext.getProtocol());
    return sslContext;
  }

  private static boolean hostnameVerificationDisabledValue() {
    var prop = getJdkInternalHttpClientDisableHostNameVerification();
    if (prop == null) {
      return false;
    }
    return Boolean.parseBoolean(prop);
  }
}
