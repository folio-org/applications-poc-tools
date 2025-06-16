package org.folio.tools.store.utils;

import static org.apache.commons.lang3.StringUtils.defaultString;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TlsProperties {

  boolean enabled;
  String trustStorePath;
  String trustStorePassword;
  String trustStoreType;

  public String getTrustStorePassword() {
    return defaultString(trustStorePassword);
  }
}
