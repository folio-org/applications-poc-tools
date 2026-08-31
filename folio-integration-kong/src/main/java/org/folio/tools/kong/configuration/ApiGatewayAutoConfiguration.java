package org.folio.tools.kong.configuration;

import static org.folio.common.utils.tls.HttpClientTlsUtils.buildHttpServiceClient;

import org.folio.tools.kong.client.KongAdminClient;
import org.folio.tools.kong.service.ApiGatewayModuleRegistrar;
import org.folio.tools.kong.service.KongGatewayService;
import org.folio.tools.kong.service.KongRouteTenantService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty("application.apigw.enabled")
@EnableConfigurationProperties(ApiGatewayConfigurationProperties.class)
public class ApiGatewayAutoConfiguration {

  /**
   * Creates a {@link KongAdminClient} HTTP Service Client for integration with Kong Admin API.
   *
   * @param properties - API Gateway configuration properties with required data
   * @return created {@link KongAdminClient} component
   */
  @Bean(name = "folioKongAdminClient")
  @ConditionalOnMissingBean(KongAdminClient.class)
  public KongAdminClient folioKongIntegrationClient(ApiGatewayConfigurationProperties properties,
    JsonMapper jsonMapper) {
    var restClientBuilder = RestClient.builder()
      .configureMessageConverters(converters -> converters
        .registerDefaults()
        .withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper)));

    return buildHttpServiceClient(restClientBuilder, properties.getTls(), properties.getUrl(),
      KongAdminClient.class);
  }

  /**
   * Creates {@link KongRouteTenantService} bean.
   *
   * @return created {@link KongRouteTenantService} bean
   */
  @Bean(name = "folioKongRouteTenantService")
  @ConditionalOnMissingBean(KongRouteTenantService.class)
  public KongRouteTenantService kongRouteTenantService() {
    return new KongRouteTenantService();
  }

  /**
   * Creates {@link KongGatewayService} bean.
   *
   * @param kongAdminClient - {@link KongAdminClient} bean from spring context
   * @param kongRouteTenantService - {@link KongRouteTenantService} bean from spring context
   * @return created {@link KongGatewayService} bean
   */
  @Bean(name = "folioKongGatewayService")
  @ConditionalOnMissingBean(KongGatewayService.class)
  public KongGatewayService kongGatewayService(KongAdminClient kongAdminClient,
    KongRouteTenantService kongRouteTenantService) {
    return new KongGatewayService(kongAdminClient, kongRouteTenantService);
  }

  /**
   * Creates {@link ApiGatewayModuleRegistrar} bean.
   *
   * @param kongGatewayService - {@link KongGatewayService} bean from spring context
   * @param objectMapper - {@link ObjectMapper} bean from spring context
   * @param resourceLoader - {@link ResourceLoader} bean from spring context
   * @return created {@link ApiGatewayModuleRegistrar} bean
   */
  @Bean(name = "folioApiGatewayModuleRegistrar")
  @ConditionalOnProperty("application.apigw.register-module")
  public ApiGatewayModuleRegistrar apiGatewayModuleRegistrar(KongGatewayService kongGatewayService,
    ObjectMapper objectMapper, ResourceLoader resourceLoader,
    ApiGatewayConfigurationProperties configurationProperties) {
    return new ApiGatewayModuleRegistrar(objectMapper, resourceLoader, kongGatewayService, configurationProperties);
  }
}
