package org.folio.security.integration.keycloak.configuration;

import static org.folio.common.utils.tls.HttpClientTlsUtils.buildHttpServiceClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.security.integration.keycloak.client.KeycloakAdminClient;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.integration.keycloak.service.KeycloakAdminTokenProvider;
import org.folio.security.integration.keycloak.service.KeycloakImportService;
import org.folio.security.integration.keycloak.service.KeycloakModuleDescriptorMapper;
import org.folio.security.integration.keycloak.service.SecureStoreKeyProvider;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@Configuration
@ConditionalOnProperty({"application.keycloak.enabled", "application.keycloak.import.enabled"})
@EnableConfigurationProperties(KeycloakProperties.class)
@RequiredArgsConstructor
public class KeycloakDataImportConfiguration {

  private final KeycloakProperties properties;
  private final SecureStore secureStore;
  private final SecureStoreKeyProvider secureStoreKeyProvider;

  /**
   * Spring HTTP Interface client for the Keycloak Admin REST API. Replaces the RESTEasy-based
   * {@code keycloak-admin-client}. The admin bearer token is acquired and cached by
   * {@link KeycloakAdminTokenProvider} (using the {@code master} realm token endpoint) and injected
   * via a {@code RestClient} request interceptor on every admin call.
   */
  @Bean
  public KeycloakAdminClient keycloakAdminClient() {
    var authClient = buildHttpServiceClient(RestClient.builder(), properties.getTls(), properties.getUrl(),
      KeycloakAuthClient.class);
    var tokenProvider = new KeycloakAdminTokenProvider(authClient, properties, this::findSecret);
    var builder = RestClient.builder().requestInterceptor((request, body, execution) -> {
      request.getHeaders().setBearerAuth(tokenProvider.getAccessToken());
      return execution.execute(request, body);
    });
    return buildHttpServiceClient(builder, properties.getTls(), properties.getUrl(), KeycloakAdminClient.class);
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
  public KeycloakImportService keycloakImportService(KeycloakAdminClient keycloakAdminClient,
    InternalModuleDescriptorProvider descriptorProvider, KeycloakModuleDescriptorMapper mapper) {
    return new KeycloakImportService(keycloakAdminClient, properties, descriptorProvider, mapper);
  }

  private String findSecret() {
    var admin = properties.getAdmin();
    String clientId = admin.getClientId();
    String secret = null;
    var key = secureStoreKeyProvider.globalStoreKey(clientId);
    try {
      secret = secureStore.get(key);
    } catch (SecretNotFoundException e) {
      log.warn("Secret for key '{}' for 'admin' client is not defined in the secret store: clientId = {}",
          key, clientId);
    }
    return secret;
  }
}
