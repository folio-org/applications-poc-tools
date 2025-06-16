package org.folio.tools.store.properties;

import static java.lang.Boolean.parseBoolean;

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
  public static final boolean DEFAULT_ENABLE_SSL = false;
  public static final String DEFAULT_TRUSTSTORE_FILE_TYPE = "jks";

  public static final String PROP_FSSP_ADDRESS = "address";
  public static final String PROP_FSSP_SECRET_PATH = "secretPath";
  public static final String PROP_FSSP_ENABLE_SSL = "enableSsl";
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
   * Whether to use SSL.
   */
  @Builder.Default
  private Boolean enableSsl = DEFAULT_ENABLE_SSL;

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
      .enableSsl(parseBoolean(properties.getProperty(PROP_FSSP_ENABLE_SSL, String.valueOf(DEFAULT_ENABLE_SSL))))
      .trustStorePath(properties.getProperty(PROP_FSSP_TRUSTSTORE_PATH))
      .trustStoreFileType(properties.getProperty(PROP_FSSP_TRUSTSTORE_FILE_TYPE, DEFAULT_TRUSTSTORE_FILE_TYPE))
      .trustStorePassword(properties.getProperty(PROP_FSSP_TRUSTSTORE_PASSWORD))
      .build();
  }
}
