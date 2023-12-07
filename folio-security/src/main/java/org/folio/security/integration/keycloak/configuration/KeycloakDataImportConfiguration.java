package org.folio.security.integration.keycloak.configuration;

import static org.apache.commons.lang3.StringUtils.stripToNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.integration.keycloak.service.KeycloakImportService;
import org.folio.security.integration.keycloak.service.KeycloakModuleDescriptorMapper;
import org.folio.security.integration.keycloak.utils.KeycloakSecretUtils;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.NotFoundException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Log4j2
@Configuration
@ConditionalOnProperty({"application.keycloak.enabled", "application.keycloak.import.enabled"})
@EnableConfigurationProperties(KeycloakProperties.class)
@RequiredArgsConstructor
public class KeycloakDataImportConfiguration {

  private final KeycloakProperties properties;
  private final SecureStore secureStore;

  @Bean
  public Keycloak keycloak() {
    var admin = properties.getAdmin();

    String clientId = admin.getClientId();

    String secret = null;
    try {
      secret = secureStore.get(KeycloakSecretUtils.globalStoreKey(clientId));
    } catch (NotFoundException e) {
      log.debug("Secret for 'admin' client is not defined in the secret store: clientId = {}", clientId);
    }

    return KeycloakBuilder.builder()
      .realm("master")
      .serverUrl(properties.getUrl())
      .clientId(clientId)
      .clientSecret(stripToNull(secret))
      .username(stripToNull(admin.getUsername()))
      .password(stripToNull(admin.getPassword()))
      .grantType(admin.getGrantType())
      .build();
  }

  @Bean
  @ConditionalOnMissingBean
  public InternalModuleDescriptorProvider moduleDescriptorProvider(ResourceLoader resourceLoader,
    ObjectMapper objectMapper) {
    return new InternalModuleDescriptorProvider(resourceLoader, objectMapper);
  }

  @Bean
  public KeycloakModuleDescriptorMapper moduleDescriptorMapper() {
    return new KeycloakModuleDescriptorMapper();
  }

  @Bean
  public KeycloakImportService keycloakImportService(Keycloak keycloakClient,
    InternalModuleDescriptorProvider descriptorProvider, KeycloakModuleDescriptorMapper mapper) {
    return new KeycloakImportService(keycloakClient, properties, descriptorProvider, mapper);
  }
}
