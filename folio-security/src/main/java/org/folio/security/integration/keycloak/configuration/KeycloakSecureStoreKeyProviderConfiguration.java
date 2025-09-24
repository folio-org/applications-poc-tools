package org.folio.security.integration.keycloak.configuration;

import org.folio.security.integration.keycloak.service.SecureStoreKeyProvider;
import org.folio.tools.store.properties.SecureStoreProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakSecureStoreKeyProviderConfiguration {

  @Bean
  public SecureStoreKeyProvider storeKeyProvider(SecureStoreProperties secureStoreProperties) {
    return new SecureStoreKeyProvider(secureStoreProperties);
  }
}
