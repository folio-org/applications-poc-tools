package org.folio.common.utils.tls;

import static org.apache.commons.lang3.SystemProperties.JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.common.utils.tls.Utils.buildSslContext;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyStoreException;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.common.utils.exception.SslInitializationException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class UtilsTest {

  private String originalPropertyValue;

  @BeforeEach
  void setUp() {
    // Store the original property value to restore it after each test
    originalPropertyValue = System.getProperty(JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION);
  }

  @AfterEach
  void tearDown() {
    // Restore the original property value
    if (originalPropertyValue == null) {
      System.clearProperty(JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION);
    } else {
      System.setProperty(JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION, originalPropertyValue);
    }
  }

  @Test
  void testHostnameVerificationDisabledValue_WithNullProperty() throws Exception {
    System.clearProperty(JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION);
    boolean result = invokeHostnameVerificationDisabledValue();
    assertFalse(result, "Expected hostname verification to be enabled when property is not set (null).");
  }

  @Test
  void testHostnameVerificationDisabledValue_WithFalseProperty() throws Exception {
    System.setProperty(JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION, "false");
    boolean result = invokeHostnameVerificationDisabledValue();
    assertFalse(result, "Expected hostname verification to be enabled when property is set to 'false'.");
  }

  @Test
  void testHostnameVerificationDisabledValue_WithTrueProperty() throws Exception {
    System.setProperty(JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION, "true");
    boolean result = invokeHostnameVerificationDisabledValue();
    assertTrue(result, "Expected hostname verification to be disabled when property is set to 'true'.");
  }

  @Test
  void buildSslContext_positive_withValidTlsProperties() {
    var tls = TlsProperties.of(true, "classpath:certificates/test.truststore.jks",
      "secretpassword", "JKS");

    var sslContext = buildSslContext(tls);

    assertThat(sslContext).isNotNull();
    assertThat(sslContext.getProtocol()).isEqualTo("TLS");
  }

  @Test
  void buildSslContext_negative_withNullTlsProperties() {
    assertThatThrownBy(() -> buildSslContext(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("TLS properties must not be null");
  }

  @Test
  void buildSslContext_negative_withNullTrustStorePath() {
    var tls = TlsProperties.of(true, null, "secretpassword", "JKS");

    assertThatThrownBy(() -> buildSslContext(tls))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Trust store path is not defined");
  }

  @Test
  void buildSslContext_negative_withInvalidTrustStorePath() {
    var tls = TlsProperties.of(true, "classpath:certificates/nonexistent.jks", "secretpassword", "JKS");

    assertThatThrownBy(() -> buildSslContext(tls))
      .isInstanceOf(SslInitializationException.class)
      .hasMessage("Error creating SSL context")
      .hasCauseInstanceOf(FileNotFoundException.class);
  }

  @Test
  void buildSslContext_negative_withInvalidPassword() {
    var tls = TlsProperties.of(true, "classpath:certificates/test.truststore.jks", "wrongpassword", "JKS");

    assertThatThrownBy(() -> buildSslContext(tls))
      .isInstanceOf(SslInitializationException.class)
      .hasMessage("Error creating SSL context")
      .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void buildSslContext_negative_withInvalidTrustStoreType() {
    var tls = TlsProperties.of(true, "classpath:certificates/test.truststore.jks", "secretpassword", "INVALID");

    assertThatThrownBy(() -> buildSslContext(tls))
      .isInstanceOf(SslInitializationException.class)
      .hasMessage("Error creating SSL context")
      .hasCauseInstanceOf(KeyStoreException.class);
  }

  private boolean invokeHostnameVerificationDisabledValue()
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = Utils.class.getDeclaredMethod("hostnameVerificationDisabledValue");
    method.setAccessible(true);
    return (boolean) method.invoke(null);
  }
}
