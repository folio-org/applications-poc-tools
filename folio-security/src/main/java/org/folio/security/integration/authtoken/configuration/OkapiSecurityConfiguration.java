package org.folio.security.integration.authtoken.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.security.integration.authtoken.client.AuthtokenClient;
import org.folio.security.integration.authtoken.configuration.properties.OkapiProperties;
import org.folio.security.integration.authtoken.service.OkapiAuthorizationService;
import org.folio.security.service.AuthorizationService;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.folio.security.service.RoutingEntryMatcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.util.UrlPathHelper;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@ConditionalOnExpression(
  "${application.security.enabled} && ${application.okapi.enabled} && !${application.keycloak.enabled}")
@EnableConfigurationProperties(OkapiProperties.class)
@RequiredArgsConstructor
public class OkapiSecurityConfiguration {

  private final OkapiProperties properties;

  @Bean
  public AuthtokenClient authtokenClient(RestClient.Builder restClientBuilder) {
    var restClient = restClientBuilder
      .baseUrl(properties.getModAuthtokenUrl())
      .build();

    var adapter = RestClientAdapter.create(restClient);
    var factory = HttpServiceProxyFactory.builderFor(adapter).build();

    return factory.createClient(AuthtokenClient.class);
  }

  @Bean
  public AuthorizationService authorizationService(AuthtokenClient authtokenClient, ObjectMapper objectMapper,
    InternalModuleDescriptorProvider internalModuleDescriptorProvider, RoutingEntryMatcher routingEntryMatcher,
    UrlPathHelper urlPathHelper, Environment environment) {
    var okapiAuthorizationService = new OkapiAuthorizationService(properties.getUrl(),
      objectMapper, authtokenClient, routingEntryMatcher, internalModuleDescriptorProvider);
    okapiAuthorizationService.setEnvironment(environment);
    okapiAuthorizationService.setUrlPathHelper(urlPathHelper);
    return okapiAuthorizationService;
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

  @Bean
  @ConditionalOnMissingBean
  public UrlPathHelper urlPathHelper() {
    return new UrlPathHelper();
  }
}
