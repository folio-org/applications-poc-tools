package org.folio.tools.store.utils;

import static org.apache.commons.lang3.StringUtils.defaultString;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TlsProperties {

  boolean enabled;
  Store keyStore;
  Store trustStore;

  @Value
  @Builder
  public static class Store {

    String path;
    String password;
    String type;

    public String getPassword() {
      return defaultString(password);
    }
  }
}
