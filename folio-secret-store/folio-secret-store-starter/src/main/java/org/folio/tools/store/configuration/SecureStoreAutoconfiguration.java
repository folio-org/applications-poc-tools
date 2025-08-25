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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecureStoreAutoconfiguration {

  @Bean
  @ConfigurationProperties(prefix = "application.secret-store")
  public SecureStoreProperties secureStoreProperties() {
    return new SecureStoreProperties();
  }

  @Bean
  @ConditionalOnProperty(name = "application.secret-store.type", havingValue = "EPHEMERAL", matchIfMissing = true)
  public SecureStore ephemeralStore(EphemeralProperties properties) {
    return EphemeralStore.create(properties);
  }

  @Bean
  @ConditionalOnProperty(name = "application.secret-store.type", havingValue = "EPHEMERAL", matchIfMissing = true)
  @ConfigurationProperties(prefix = "application.secret-store.ephemeral")
  public EphemeralProperties ephemeralProperties() {
    return new EphemeralProperties();
  }

  @Bean
  @ConditionalOnProperty(name = "application.secret-store.type", havingValue = "AWS_SSM")
  public SecureStore awsStore(AwsProperties properties) {
    return AwsStore.create(properties);
  }

  @Bean
  @ConditionalOnProperty(name = "application.secret-store.type", havingValue = "AWS_SSM")
  @ConfigurationProperties(prefix = "application.secret-store.aws-ssm")
  public AwsProperties awsProperties() {
    return new AwsProperties();
  }

  @Bean
  @ConditionalOnProperty(name = "application.secret-store.type", havingValue = "VAULT")
  public SecureStore vaultStore(VaultProperties properties) {
    return VaultStore.create(properties);
  }

  @Bean
  @ConditionalOnProperty(name = "application.secret-store.type", havingValue = "VAULT")
  @ConfigurationProperties(prefix = "application.secret-store.vault")
  public VaultProperties vaultProperties() {
    return new VaultProperties();
  }

  @Bean
  @ConditionalOnProperty(name = "application.secret-store.type", havingValue = "FSSP")
  public SecureStore fsspStore(FsspProperties properties) {
    return new FsspStore(properties);
  }

  @Bean
  @ConditionalOnProperty(name = "application.secret-store.type", havingValue = "FSSP")
  @ConfigurationProperties(prefix = "application.secret-store.fssp")
  public FsspProperties fsspProperties() {
    return new FsspProperties();
  }
}
