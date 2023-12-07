package org.folio.tools.store.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultConfigProperties {

  public static final String DEFAULT_VAULT_SECRET_ROOT = "secret";
  /**
   * Token for accessing vault, may be a root token.
   */
  private String token;

  /**
   * The address of your vault.
   */
  private String address;

  /**
   * Root path for secrets.
   */
  @Builder.Default
  private String secretRoot = DEFAULT_VAULT_SECRET_ROOT;

  /**
   * Whether to use SSL.
   */
  private Boolean enableSsl;

  /**
   * The path to an X.509 certificate in unencrypted PEM format, using UTF-8 encoding.
   */
  private String pemFilePath;

  /**
   * The password used to access the JKS keystore (optional).
   */
  private String keystorePassword;

  /**
   * The path to a JKS keystore file containing a client cert and private key.
   */
  private String keystoreFilePath;

  /**
   * The path to a JKS truststore file containing Vault server certs that can be trusted.
   */
  private String truststoreFilePath;
}
