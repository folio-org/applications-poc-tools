package org.folio.tools.store.properties;

import java.util.Properties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FsspConfigProperties {

  public static final String DEFAULT_SECRET_PATH = "secure-store/entries";
  public static final String DEFAULT_TRUSTSTORE_FILE_TYPE = "jks";

  public static final String PROP_FSSP_ADDRESS = "address";
  public static final String PROP_FSSP_SECRET_PATH = "secretPath";
  public static final String PROP_FSSP_KEYSTORE_PATH = "keyStorePath";
  public static final String PROP_FSSP_KEYSTORE_FILE_TYPE = "keyStoreFileType";
  public static final String PROP_FSSP_KEYSTORE_PASSWORD = "keyStorePassword";
  public static final String PROP_FSSP_TRUSTSTORE_PATH = "trustStorePath";
  public static final String PROP_FSSP_TRUSTSTORE_FILE_TYPE = "trustStoreFileType";
  public static final String PROP_FSSP_TRUSTSTORE_PASSWORD = "trustStorePassword";

  /**
   * The address of Folio Secure Store Proxy (FSSP).
   */
  private String address;

  /**
   * The root path for secrets.
   */
  @Builder.Default
  private String secretPath = DEFAULT_SECRET_PATH;

  /**
   * The path to the key store file.
   */
  private String keyStorePath;

  /**
   * The password for the key store file.
   */
  private String keyStorePassword;

  /**
   * The type of the key store file (e.g., "jks", "pem").
   */
  @Builder.Default
  private String keyStoreFileType = DEFAULT_TRUSTSTORE_FILE_TYPE;

  /**
   * The path to the trust store file.
   */
  private String trustStorePath;

  /**
   * The password for the trust store file.
   */
  private String trustStorePassword;

  /**
   * The type of the trust store file (e.g., "jks", "pem").
   */
  @Builder.Default
  private String trustStoreFileType = DEFAULT_TRUSTSTORE_FILE_TYPE;

  public static FsspConfigProperties from(Properties properties) {
    return builder()
      .address(properties.getProperty(PROP_FSSP_ADDRESS))
      .secretPath(properties.getProperty(PROP_FSSP_SECRET_PATH, DEFAULT_SECRET_PATH))
      .keyStorePath(properties.getProperty(PROP_FSSP_KEYSTORE_PATH))
      .keyStoreFileType(properties.getProperty(PROP_FSSP_KEYSTORE_FILE_TYPE, DEFAULT_TRUSTSTORE_FILE_TYPE))
      .keyStorePassword(properties.getProperty(PROP_FSSP_KEYSTORE_PASSWORD))
      .trustStorePath(properties.getProperty(PROP_FSSP_TRUSTSTORE_PATH))
      .trustStoreFileType(properties.getProperty(PROP_FSSP_TRUSTSTORE_FILE_TYPE, DEFAULT_TRUSTSTORE_FILE_TYPE))
      .trustStorePassword(properties.getProperty(PROP_FSSP_TRUSTSTORE_PASSWORD))
      .build();
  }
}
