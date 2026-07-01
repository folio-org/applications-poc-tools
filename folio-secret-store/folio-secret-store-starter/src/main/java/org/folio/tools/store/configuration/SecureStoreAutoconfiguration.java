package org.folio.tools.store.configuration;

import org.folio.tools.store.SecureStore;
import org.folio.tools.store.impl.AwsStore;
import org.folio.tools.store.impl.EphemeralStore;
import org.folio.tools.store.impl.FsspStore;
import org.folio.tools.store.impl.VaultStore;
import org.folio.tools.store.properties.AwsProperties;
import org.folio.tools.store.properties.EphemeralProperties;
import org.folio.tools.store.properties.FsspProperties;
import org.folio.tools.store.properties.SecureStoreProperties;
import org.folio.tools.store.properties.VaultProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Secure-store auto-configuration.
 *
 * <p>Backend selection is done at <b>runtime</b> via {@code application.secret-store.type} instead of the
 * previous build-time {@code @ConditionalOnProperty} beans. Build-time conditions are evaluated during Spring
 * AOT / native-image processing, which would bake a single backend into the image and make the same binary
 * unable to switch backends by configuration. All backend property beans are registered unconditionally, and
 * the single {@link SecureStore} bean references every implementation (so all are reachable for native image)
 * but constructs only the selected one — important because {@link VaultStore}/{@link FsspStore} build clients
 * and validate configuration in their constructors and would fail if instantiated while unconfigured.</p>
 */
@Configuration
public class SecureStoreAutoconfiguration {

  @Bean
  @ConfigurationProperties(prefix = "application.secret-store")
  public SecureStoreProperties secureStoreProperties() {
    return new SecureStoreProperties();
  }

  @Bean
  @ConfigurationProperties(prefix = "application.secret-store.ephemeral")
  public EphemeralProperties ephemeralProperties() {
    return new EphemeralProperties();
  }

  @Bean
  @ConfigurationProperties(prefix = "application.secret-store.aws-ssm")
  public AwsProperties awsProperties() {
    return new AwsProperties();
  }

  @Bean
  @ConfigurationProperties(prefix = "application.secret-store.vault")
  public VaultProperties vaultProperties() {
    return new VaultProperties();
  }

  @Bean
  @ConfigurationProperties(prefix = "application.secret-store.fssp")
  public FsspProperties fsspProperties() {
    return new FsspProperties();
  }

  /**
   * The active {@link SecureStore}, selected at runtime from {@code application.secret-store.type}. Only the
   * chosen backend is instantiated; the other implementations remain reachable (in the native image) via the
   * references in this method.
   */
  @Bean
  public SecureStore secureStore(SecureStoreProperties secureStoreProperties,
    EphemeralProperties ephemeralProperties, AwsProperties awsProperties,
    VaultProperties vaultProperties, FsspProperties fsspProperties) {
    return switch (secureStoreProperties.getType()) {
      case AWS_SSM -> AwsStore.create(awsProperties);
      case VAULT -> VaultStore.create(vaultProperties);
      case FSSP -> new FsspStore(fsspProperties);
      case EPHEMERAL -> EphemeralStore.create(ephemeralProperties);
    };
  }
}
