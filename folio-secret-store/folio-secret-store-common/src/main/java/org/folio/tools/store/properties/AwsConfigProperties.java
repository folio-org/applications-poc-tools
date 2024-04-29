package org.folio.tools.store.properties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsConfigProperties {

  /**
   * The AWS region to pass to the AWS SSM Client Builder.
   *
   * <p> If not set, the AWS Default Region Provider Chain is used to determine which region to use. </p>
   */
  private String region;

  /**
   * If true, will rely on the current IAM role for authorization instead of explicitly providing AWS credentials
   * (access_key/secret_key).
   */
  private Boolean useIam;

  /**
   * The HTTP endpoint to use for retrieving AWS credentials.
   */
  private String ecsCredentialsEndpoint;

  /**
   * The path component of the credentials' endpoint URI.
   *
   * <p> This value is appended to the credentials' endpoint to form the URI from which credentials can be obtained.
   * </p>
   *
   * <p> If omitted, the value will be read from the AWS_CONTAINER_CREDENTIALS_RELATIVE_URI environment variable
   * (standard on ECS containers) </p>
   *
   * <p>You won't typically need to set this unless using AwsParamStore from outside an ECS container</p>
   */
  private String ecsCredentialsPath;

  /**
   * Defines whether FIPS mode is enabled.
   */
  private boolean fipsEnabled;

  /**
   * The path to a BCFKS truststore file containing FIPS server certs that can be trusted.
   */
  private String trustStorePath;

  /**
   * The password used to access the BCFKS truststore.
   */
  private String trustStorePassword;

  /**
   * The file type for truststore.
   */
  private String trustStoreFileType;
}
