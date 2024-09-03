package org.folio.common.utils.tls;

import static org.apache.commons.lang3.SystemProperties.getJdkInternalHttpClientDisableHostNameVerification;

import lombok.experimental.UtilityClass;

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
}
