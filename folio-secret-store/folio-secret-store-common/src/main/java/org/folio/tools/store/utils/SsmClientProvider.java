package org.folio.tools.store.utils;

import static java.lang.Boolean.parseBoolean;
import static java.util.Objects.nonNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Optional;
import java.util.Properties;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.SSLInitializationException;
import org.apache.http.util.Asserts;
import org.folio.tools.store.properties.AwsConfigProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;

@Log4j2
public class SsmClientProvider {

  public static final String PROP_REGION = "region";
  public static final String PROP_USE_IAM = "useIAM";
  public static final String PROP_ECS_CREDENTIALS_PATH = "ecsCredentialsPath";
  public static final String PROP_ECS_CREDENTIALS_ENDPOINT = "ecsCredentialsEndpoint";
  public static final String PROP_ACCESS_KEY = "accessKey";
  public static final String PROP_SECRET_KEY = "secretKey";
  public static final String DEFAULT_USE_IAM = "true";
  public static final String ECS_CREDENTIALS_PATH_VAR = "AWS_CONTAINER_CREDENTIALS_RELATIVE_URI";

  private final AwsConfigProperties properties;

  public SsmClientProvider(Properties properties) {
    this.properties = mapPropertiesToAwsConfigProperties(properties);
  }

  public SsmClientProvider(AwsConfigProperties properties) {
    this.properties = properties;
  }

  /**
   * Provides SSM client.
   *
   * @return created {@link SsmClient} object
   */
  public SsmClient get() {
    log.debug("Initializing AWS SSM client...");
    var builder = SsmClient.builder();
    builder.region(Region.of(properties.getRegion()));
    if (properties.isFipsEnabled()) {
      builder.httpClient(ApacheHttpClient.builder()
        .tlsTrustManagersProvider(() -> getTrustManager(properties)).build());
      builder.fipsEnabled(true);
    }
    if (nonNull(properties.getUseIam()) && properties.getUseIam()) {
      log.debug("Using IAM");
    } else {
      builder.credentialsProvider(getAwsCredentialsProvider(properties));
      endpoint(properties).ifPresent(builder::endpointOverride);
    }
    return builder.build();
  }

  private TrustManager[] getTrustManager(AwsConfigProperties properties) {
    Asserts.notBlank(properties.getTrustStorePath(), "Truststore path must not be blank");
    try {
      var trustStore = KeyStore.getInstance(properties.getTrustStoreFileType());
      try (var trustStoreStream = Files.newInputStream(Path.of(properties.getTrustStorePath()).toAbsolutePath())) {
        trustStore.load(trustStoreStream, properties.getTrustStorePassword().toCharArray());
      }

      var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(trustStore);
      return trustManagerFactory.getTrustManagers();
    } catch (Exception e) {
      throw new SSLInitializationException("Error initializing TrustManager", e);
    }
  }

  private AwsCredentialsProvider getAwsCredentialsProvider(AwsConfigProperties properties) {
    try {
      return StaticCredentialsProvider.create(
        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey()));
    } catch (Exception e) {
      log.info("Using DefaultCredentialsProvider", e);
      return DefaultCredentialsProvider.create();
    }
  }

  private static Optional<URI> endpoint(AwsConfigProperties properties) {
    var path = properties.getEcsCredentialsPath();
    if (path == null) {
      path = System.getenv(ECS_CREDENTIALS_PATH_VAR);
    }
    if (path == null || StringUtils.isBlank(properties.getEcsCredentialsEndpoint())) {
      return Optional.empty();
    }

    try {
      return Optional.of(new URI(properties.getEcsCredentialsEndpoint() + path));
    } catch (URISyntaxException e) {
      log.warn("Error creating URI", e);
      throw SdkClientException.builder().cause(e).build();
    }
  }

  private static AwsConfigProperties mapPropertiesToAwsConfigProperties(Properties properties) {
    return AwsConfigProperties.builder()
      .useIam(parseBoolean(properties.getProperty(PROP_USE_IAM, DEFAULT_USE_IAM)))
      .region(properties.getProperty(PROP_REGION))
      .accessKey(properties.getProperty(PROP_ACCESS_KEY))
      .secretKey(properties.getProperty(PROP_SECRET_KEY))
      .ecsCredentialsPath(properties.getProperty(PROP_ECS_CREDENTIALS_PATH))
      .ecsCredentialsEndpoint(properties.getProperty(PROP_ECS_CREDENTIALS_ENDPOINT))
      .build();
  }
}
