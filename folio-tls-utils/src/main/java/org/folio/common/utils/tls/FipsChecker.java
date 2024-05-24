package org.folio.common.utils.tls;

import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.stream.Stream;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

public final class FipsChecker {

  public static final String JAVA_SECURITY_PROPERTIES = "java.security.properties";
  public static final String KEYSTORE_TYPE_COMPAT = "keystore.type.compat";
  public static final String JAVAX_NET_SSL_TRUST_STORE_TYPE = "javax.net.ssl.trustStoreType";
  public static final String JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore";
  public static final String JAVAX_NET_SSL_TRUST_STORE_PROVIDER = "javax.net.ssl.trustStoreProvider";
  public static final String JAVAX_NET_SSL_KEY_STORE_TYPE = "javax.net.ssl.keyStoreType";
  public static final String JAVAX_NET_SSL_KEY_STORE = "javax.net.ssl.keyStore";
  public static final String JAVAX_NET_SSL_KEY_STORE_PROVIDER = "javax.net.ssl.keyStoreProvider";
  public static final String ORG_BOUNCYCASTLE_FIPS_APPROVED_ONLY = "org.bouncycastle.fips.approved_only";
  public static final String[] ENV_VARS =
    new String[] {KEYSTORE_TYPE_COMPAT, JAVAX_NET_SSL_TRUST_STORE_TYPE, JAVAX_NET_SSL_TRUST_STORE,
      JAVAX_NET_SSL_TRUST_STORE_PROVIDER, JAVAX_NET_SSL_KEY_STORE_TYPE, JAVAX_NET_SSL_KEY_STORE,
      JAVAX_NET_SSL_KEY_STORE_PROVIDER, ORG_BOUNCYCASTLE_FIPS_APPROVED_ONLY};

  private FipsChecker() {
  }

  public static String isInBouncycastleApprovedOnlyMode() {
    try {
      Class<?> clazz =
        FipsChecker.class.getClassLoader().loadClass("org.bouncycastle.crypto.CryptoServicesRegistrar");
      Method isInApprovedOnlyMode = clazz.getDeclaredMethod("isInApprovedOnlyMode");
      boolean isEnabled = (boolean) isInApprovedOnlyMode.invoke(null);
      return isEnabled ? "Enabled" : "Disabled";
    } catch (Throwable ignore) {
      System.out.println("Could not detect org.bouncycastle.crypto.CryptoServicesRegistrar: " + ignore);
      return "Unknown";
    }
  }

  public static String isSystemFipsEnabled() {
    Method isSystemFipsEnabled = null;

    try {
      Class<?> securityConfigurator =
        FipsChecker.class.getClassLoader().loadClass("java.security.SystemConfigurator");
      isSystemFipsEnabled = securityConfigurator.getDeclaredMethod("isSystemFipsEnabled");
      isSystemFipsEnabled.setAccessible(true);
      boolean isEnabled = (boolean) isSystemFipsEnabled.invoke(null);
      return isEnabled ? "Enabled" : "Disabled";
    } catch (Throwable ignore) {
      System.out.println("Could not detect if FIPS is enabled from the host: " + ignore);
      return "Unknown";
    } finally {
      if (isSystemFipsEnabled != null) {
        isSystemFipsEnabled.setAccessible(false);
      }
    }
  }

  public static String dumpJavaSecurityProviders() {
    StringBuilder builder = new StringBuilder("Java security providers: [\n");
    for (Provider p : Security.getProviders()) {
      builder.append("  ").append(p.toString()).append(" - ").append(p.getClass()).append(",\n");
    }
    return builder.append("]").toString();
  }

  public static String dumpSecurityProperties() {
    StringBuilder builder = new StringBuilder("Security properties: [\n  Java security properties file: ")
      .append(System.getProperty(JAVA_SECURITY_PROPERTIES))
      .append("\n")
      .append("  Default keystore type: ").append(KeyStore.getDefaultType())
      .append("\n")
      .append("  KeyManagerFactory.getDefaultAlgorithm(): ").append(KeyManagerFactory.getDefaultAlgorithm())
      .append("\n")
      .append("  TrustManagerFactory.getDefaultAlgorithm(): ").append(TrustManagerFactory.getDefaultAlgorithm())
      .append("\n\n");
    Stream.of(ENV_VARS).forEach(propertyName ->
      builder.append("  ").append(propertyName).append(": ").append(System.getProperty(propertyName)).append("\n"));
    return builder.append("]").toString();
  }

  public static String getFipsChecksResultString() {
    return "\n(" + "BC Approved Only Mode: " + isInBouncycastleApprovedOnlyMode()
      + ", FIPS-JVM: " + isSystemFipsEnabled() + ")\n"
      + dumpJavaSecurityProviders()
      + dumpSecurityProperties();
  }
}
