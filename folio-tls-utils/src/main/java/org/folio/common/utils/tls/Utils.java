package org.folio.common.utils.tls;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.SystemProperties.getJdkInternalHttpClientDisableHostNameVerification;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.common.utils.exception.SslInitializationException;
import org.springframework.util.ResourceUtils;

@Log4j2
@UtilityClass
public class Utils {

  public static final boolean IS_HOSTNAME_VERIFICATION_DISABLED = // enabled by default
    hostnameVerificationDisabledValue();

  private static boolean hostnameVerificationDisabledValue() {
    String prop = getJdkInternalHttpClientDisableHostNameVerification();
    if (prop == null) {
      return false;
    }
    return Boolean.parseBoolean(prop);
  }

  /**
   * Builds an SSLContext from the provided TLS properties.
   *
   * @param tls TLS configuration properties
   * @return configured SSLContext
   * @throws SslInitializationException if SSL context cannot be created
   */
  public static SSLContext buildSslContext(TlsProperties tls) {
    Objects.requireNonNull(tls, "TLS properties must not be null");
    Objects.requireNonNull(tls.getTrustStorePath(), "Trust store path is not defined");
    try {
      var keyStore = initKeyStore(tls);
      var trustManager = trustManager(keyStore);
      return sslContext(trustManager);
    } catch (Exception e) {
      log.error("Error creating SSL context", e);
      throw new SslInitializationException("Error creating SSL context", e);
    }
  }

  /**
   * Initializes a KeyStore from the provided TLS properties.
   *
   * @param tls TLS configuration properties
   * @return initialized KeyStore
   * @throws KeyStoreException if keystore cannot be loaded
   * @throws CertificateException if certificate is invalid
   * @throws NoSuchAlgorithmException if algorithm is not supported
   */
  private static KeyStore initKeyStore(TlsProperties tls)
    throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {

    KeyStore trustStore = KeyStore.getInstance(
      isBlank(tls.getTrustStoreType()) ? KeyStore.getDefaultType() : tls.getTrustStoreType());
    try (var is = new FileInputStream(ResourceUtils.getFile(tls.getTrustStorePath()))) {
      trustStore.load(is, tls.getTrustStorePassword().toCharArray());
    }
    log.debug("Keystore initialized from file: keyStoreType = {}, file = {}", trustStore.getType(),
      tls.getTrustStorePath());
    return trustStore;
  }

  /**
   * Creates an X509TrustManager from the provided KeyStore.
   *
   * @param keyStore the keystore containing trusted certificates
   * @return configured X509TrustManager
   * @throws NoSuchAlgorithmException if algorithm is not supported
   * @throws KeyStoreException if keystore cannot be accessed
   */
  private static X509TrustManager trustManager(KeyStore keyStore) throws NoSuchAlgorithmException, KeyStoreException {
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(keyStore);

    TrustManager[] trustManagers = tmf.getTrustManagers();
    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
      throw new IllegalStateException("Unexpected default trust managers: " + Arrays.toString(trustManagers));
    }
    return (X509TrustManager) trustManagers[0];
  }

  /**
   * Creates an SSLContext from the provided X509TrustManager.
   *
   * @param trustManager the trust manager to use
   * @return configured SSLContext
   * @throws NoSuchAlgorithmException if algorithm is not supported
   * @throws KeyManagementException if key management fails
   */
  private static SSLContext sslContext(X509TrustManager trustManager)
    throws NoSuchAlgorithmException, KeyManagementException {

    var sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, new TrustManager[] {trustManager}, null);
    log.debug("SSL context initialized: protocol = {}", sslContext.getProtocol());
    return sslContext;
  }
}
