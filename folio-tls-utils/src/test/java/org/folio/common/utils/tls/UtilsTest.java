package org.folio.common.utils.tls;

import static org.apache.commons.lang3.SystemProperties.JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  private boolean invokeHostnameVerificationDisabledValue()
    throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = Utils.class.getDeclaredMethod("hostnameVerificationDisabledValue");
    method.setAccessible(true);
    return (boolean) method.invoke(null);
  }
}
