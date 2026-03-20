package org.folio.security.integration.keycloak.configuration;

import static org.folio.common.utils.tls.HttpClientTlsUtils.buildHttpServiceClient;

import lombok.RequiredArgsConstructor;
import org.folio.jwt.openid.JsonWebTokenParser;
import org.folio.jwt.openid.OpenidJwtParserProvider;
import org.folio.jwt.openid.configuration.JwtParserConfiguration;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.integration.keycloak.service.KeycloakAuthorizationService;
import org.folio.security.integration.keycloak.service.KeycloakPublicKeyProvider;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.folio.security.service.RoutingEntryMatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UrlPathHelper;
import tools.jackson.databind.ObjectMapper;

@ConditionalOnProperty({"application.security.enabled", "application.keycloak.enabled"})
@EnableConfigurationProperties(KeycloakProperties.class)
@RequiredArgsConstructor
public class KeycloakSecurityConfiguration {

  private final KeycloakProperties properties;

  @Bean
  public KeycloakAuthClient keycloakAuthClient() {
    return buildHttpServiceClient(RestClient.builder(), properties.getTls(), properties.getUrl(),
      KeycloakAuthClient.class);
  }

  @Bean
  public KeycloakAuthorizationService authorizationService(KeycloakAuthClient keycloakClient,
    RoutingEntryMatcher routingEntryMatcher, JsonWebTokenParser jsonWebTokenParser,
    Environment environment, UrlPathHelper urlPathHelper) {
    var service = new KeycloakAuthorizationService(properties, keycloakClient, routingEntryMatcher, jsonWebTokenParser);
    service.setEnvironment(environment);
    service.setUrlPathHelper(urlPathHelper);
    return service;
  }

  @Bean
  public KeycloakPublicKeyProvider publicKeyProvider(KeycloakAuthClient keycloakAuthClient) {
    return new KeycloakPublicKeyProvider(keycloakAuthClient);
  }

  @Bean
  public JsonWebTokenParser jsonWebTokenParser(ObjectMapper objectMapper, KeycloakProperties keycloakProperties,
    OpenidJwtParserProvider openidJwtParserProvider) {

    var jwtParserConfiguration = JwtParserConfiguration.builder()
      .validateUri(properties.getJwtCacheConfiguration().isValidateUri())
      .issuerRootUri(keycloakProperties.getUrl())
      .build();

    return new JsonWebTokenParser(objectMapper, jwtParserConfiguration, openidJwtParserProvider);
  }

  @Bean
  public OpenidJwtParserProvider openidJwtParserProvider() {
    var jwtCacheConfiguration = properties.getJwtCacheConfiguration();
    return new OpenidJwtParserProvider(
      jwtCacheConfiguration.getJwksRefreshInterval(),
      jwtCacheConfiguration.getForcedJwksRefreshInterval(),
      properties.getJwksBaseUrl());
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
