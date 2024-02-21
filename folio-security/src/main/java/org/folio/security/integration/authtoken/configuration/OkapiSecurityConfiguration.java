package org.folio.security.integration.authtoken.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
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
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.util.UrlPathHelper;

@Log4j2
@ConditionalOnExpression(
  "${application.security.enabled} && ${application.okapi.enabled} && !${application.keycloak.enabled}")
@EnableConfigurationProperties(OkapiProperties.class)
@Import(FeignClientsConfiguration.class)
@RequiredArgsConstructor
public class OkapiSecurityConfiguration {

  private final OkapiProperties properties;

  @Bean
  public AuthtokenClient authtokenClient(Contract contract, Encoder encoder, Decoder decoder) {
    return Feign.builder()
      .contract(contract).encoder(encoder).decoder(decoder)
      .target(AuthtokenClient.class, properties.getModAuthtokenUrl());
  }

  @Bean
  public AuthorizationService authorizationService(AuthtokenClient authtokenClient,
    InternalModuleDescriptorProvider internalModuleDescriptorProvider, RoutingEntryMatcher routingEntryMatcher,
    UrlPathHelper urlPathHelper, Environment environment) {
    var okapiAuthorizationService = new OkapiAuthorizationService(urlPathHelper, routingEntryMatcher,
      internalModuleDescriptorProvider, authtokenClient, properties.getUrl());
    okapiAuthorizationService.setEnvironment(environment);
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
