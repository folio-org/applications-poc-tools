package org.folio.security.integration.keycloak.configuration;

import static org.folio.common.utils.FeignClientTlsUtils.buildTargetFeignClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.RequiredArgsConstructor;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.integration.keycloak.service.KeycloakAuthorizationService;
import org.folio.security.integration.keycloak.service.KeycloakPublicKeyProvider;
import org.folio.security.integration.keycloak.service.KeycloakTokenValidator;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.folio.security.service.RoutingEntryMatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

@ConditionalOnProperty({"application.security.enabled", "application.keycloak.enabled"})
@Import(FeignClientsConfiguration.class)
@EnableConfigurationProperties(KeycloakProperties.class)
@RequiredArgsConstructor
public class KeycloakSecurityConfiguration {

  private final KeycloakProperties properties;

  @Bean
  public KeycloakAuthClient keycloakAuthClient(okhttp3.OkHttpClient okHttpClient, Contract contract, Encoder encoder,
    Decoder decoder) {
    return buildTargetFeignClient(okHttpClient, contract, encoder, decoder, properties.getTls(), properties.getUrl(),
      KeycloakAuthClient.class);
  }

  @Bean
  public KeycloakAuthorizationService authorizationService(KeycloakAuthClient keycloakClient,
    RoutingEntryMatcher routingEntryMatcher, KeycloakTokenValidator tokenValidator, Environment environment) {
    var service = new KeycloakAuthorizationService(keycloakClient, routingEntryMatcher, tokenValidator, properties);
    service.setEnvironment(environment);
    return service;
  }

  @Bean
  public KeycloakPublicKeyProvider publicKeyProvider(KeycloakAuthClient keycloakAuthClient) {
    return new KeycloakPublicKeyProvider(keycloakAuthClient);
  }

  @Bean
  public KeycloakTokenValidator tokenValidator(KeycloakPublicKeyProvider publicKeyProvider) {
    return new KeycloakTokenValidator(publicKeyProvider, properties);
  }

  @Bean
  public RoutingEntryMatcher routingEntryMatcher(InternalModuleDescriptorProvider descriptorProvider) {
    return new RoutingEntryMatcher(descriptorProvider);
  }

  @Bean
  @ConditionalOnMissingBean
  public InternalModuleDescriptorProvider moduleDescriptorProvider(ResourceLoader resourceLoader,
    ObjectMapper objectMapper) {
    return new InternalModuleDescriptorProvider(resourceLoader, objectMapper);
  }
}
