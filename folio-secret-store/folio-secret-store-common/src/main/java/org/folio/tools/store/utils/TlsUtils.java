package org.folio.tools.store.utils;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.SystemProperties.getJdkInternalHttpClientDisableHostNameVerification;
import static org.folio.tools.store.utils.ResourceUtils.getFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

@Log4j2
@UtilityClass
public class TlsUtils {

  public static final boolean IS_HOSTNAME_VERIFICATION_DISABLED = // enabled by default
    hostnameVerificationDisabledValue();

  public static SSLContext buildSslContext(TlsProperties tls) {
    requireNonNull(tls.getTrustStorePath(), "Trust store path is not defined");
    try {
      var keyStore = initKeyStore(tls);
      var trustManager = trustManager(keyStore);
      return sslContext(trustManager);
    } catch (Exception e) {
      log.error("Error creating SSL context", e);
      throw new IllegalStateException("Error creating SSL context", e);
    }
  }

  private static KeyStore initKeyStore(TlsProperties tls)
    throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

    KeyStore trustStore = KeyStore.getInstance(
      isBlank(tls.getTrustStoreType()) ? KeyStore.getDefaultType() : tls.getTrustStoreType());

    try (var is = new FileInputStream(getFile(tls.getTrustStorePath()))) {
      trustStore.load(is, tls.getTrustStorePassword().toCharArray());
    }
    log.debug("Keystore initialized from file: keyStoreType = {}, file = {}", trustStore.getType(),
      tls.getTrustStorePath());
    return trustStore;
  }

  private static X509TrustManager trustManager(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(keyStore);

    TrustManager[] trustManagers = tmf.getTrustManagers();
    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
      throw new IllegalStateException("Unexpected default trust managers: " + Arrays.toString(trustManagers));
    }
    return (X509TrustManager) trustManagers[0];
  }

  private static SSLContext sslContext(X509TrustManager trustManager)
    throws NoSuchAlgorithmException, KeyManagementException {

    var sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] {trustManager}, null);
    log.debug("SSL context initialized: protocol = {}", sslContext.getProtocol());
    return sslContext;
  }

  private static boolean hostnameVerificationDisabledValue() {
    String prop = getJdkInternalHttpClientDisableHostNameVerification();
    if (prop == null) {
      return false;
    }
    return Boolean.parseBoolean(prop);
  }
}
