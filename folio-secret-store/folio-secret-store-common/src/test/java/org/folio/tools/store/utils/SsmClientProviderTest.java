package org.folio.tools.store.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URISyntaxException;
import java.security.Security;
import java.util.Properties;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.folio.test.types.UnitTest;
import org.folio.tools.store.properties.AwsConfigProperties;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.exception.SdkClientException;

@UnitTest
class SsmClientProviderTest {

  public static final AwsConfigProperties AWS_CONFIG_PROPERTIES = AwsConfigProperties.builder()
    .region("us-east-1")
    .useIam(true)
    .fipsEnabled(true)
    .trustStorePath("src/test/resources/certificates/keystore.bcfks")
    .trustStorePassword("secretpassword")
    .trustStoreFileType("BCFKS").build();

  static {
    Security.addProvider(new BouncyCastleFipsProvider());
  }

  @Test
  void get_positive_iamDisabled() {
    var props = AwsConfigProperties.builder().region("us-east-1").accessKey("accessKey").secretKey("secretKey")
      .useIam(false).build();
    assertThat(new SsmClientProvider(props).get()).isNotNull();
  }

  @Test
  void get_positive_iamDisabled_defaultCredentialsProvider() {
    var props = AwsConfigProperties.builder().region("us-east-1").useIam(false).build();
    assertThat(new SsmClientProvider(props).get()).isNotNull();
  }

  @Test
  void get_positive() {
    var ssmClientProvider = new SsmClientProvider(AWS_CONFIG_PROPERTIES);
    var actual = ssmClientProvider.get();
    assertThat(actual).isNotNull();
  }

  @Test
  void get_positive_propertiesConfiguration() {
    var properties = new Properties();
    properties.setProperty("ecsCredentialsPath", "https://example.com/test-credentials-path");
    properties.setProperty("ecsCredentialsEndpoint", "https://example.com/test-credentials");
    properties.setProperty("region", "us-east-1");
    properties.setProperty("accessKey", "accessKey");
    properties.setProperty("secretKey", "secretKey");
    properties.setProperty("useIAM", "false");

    var ssmClientProvider = new SsmClientProvider(properties);
    var actual = ssmClientProvider.get();
    assertThat(actual).isNotNull();
  }

  @Test
  void get_negative_invalidEcsCredentialsEndpoint() {
    var properties = new Properties();
    properties.setProperty("ecsCredentialsPath", "/test-credentials");
    properties.setProperty("ecsCredentialsEndpoint", "https://example.com/q/h?s=^IXIC");
    properties.setProperty("region", "us-east-1");
    properties.setProperty("accessKey", "accessKey");
    properties.setProperty("secretKey", "secretKey");
    properties.setProperty("useIAM", "false");

    var ssmClientProvider = new SsmClientProvider(properties);
    assertThatThrownBy(ssmClientProvider::get)
      .isInstanceOf(SdkClientException.class)
      .hasCauseInstanceOf(URISyntaxException.class);
  }
}
