package org.folio.tools.store.support;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretStoreTestValues {

  public static final String KS_FILE_TYPE_PKCS12 = "PKCS12";
  public static final String KS_PASSWORD = "supersecret";

  public static final String FSSP_CLIENT_KEYSTORE_PATH = "classpath:certificates/client/fssp-user-client-keystore.p12";
  public static final String FSSP_CLIENT_TRUSTSTORE_PATH = "classpath:certificates/client/fssp-client-truststore.p12";

  public static final String FSSP_SERVER_KEYSTORE_PATH = "classpath:certificates/server/fssp-server-keystore.p12";
  public static final String FSSP_SERVER_TRUSTSTORE_PATH = "classpath:certificates/server/fssp-server-truststore.p12";
}
