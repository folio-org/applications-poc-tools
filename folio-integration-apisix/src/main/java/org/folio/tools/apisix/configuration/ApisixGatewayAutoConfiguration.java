package org.folio.tools.apisix.configuration;

import static org.folio.common.utils.tls.HttpClientTlsUtils.getRequestFactory;

import org.apache.commons.lang3.StringUtils;
import org.folio.common.gateway.ApiGatewayModuleRegistrar;
import org.folio.common.gateway.ModuleRegistrationSettings;
import org.folio.tools.apisix.client.ApisixAdminClient;
import org.folio.tools.apisix.service.ApisixGatewayService;
import org.folio.tools.apisix.service.ApisixRouteFactory;
import org.folio.tools.apisix.service.ApisixRouteTenantService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@ConditionalOnProperty("application.apigw.enabled")
@ConditionalOnProperty(name = "application.apigw.type", havingValue = "apisix")
@EnableConfigurationProperties(ApisixGatewayConfigurationProperties.class)
public class ApisixGatewayAutoConfiguration {

  /**
   * Creates an {@link ApisixAdminClient} HTTP Service Client for integration with the APISIX Admin API.
   *
   * @param properties - API Gateway configuration properties with required data
   * @return created {@link ApisixAdminClient} component
   */
  @Bean(name = "folioApisixAdminClient")
  @ConditionalOnMissingBean(ApisixAdminClient.class)
  public ApisixAdminClient folioApisixAdminClient(ApisixGatewayConfigurationProperties properties,
    JsonMapper jsonMapper) {
    if (StringUtils.isBlank(properties.getApiKey())) {
      throw new IllegalStateException(
        "application.apigw.api-key (APIGW_API_KEY) is required when application.apigw.type=apisix");
    }

    // The APISIX Admin API rejects chunked request bodies with 413; buffering ensures Content-Length is set.
    var restClient = RestClient.builder()
      .baseUrl(properties.getUrl())
      .defaultHeader("X-API-KEY", properties.getApiKey())
      .requestFactory(new BufferingClientHttpRequestFactory(apisixRequestFactory(properties)))
      .configureMessageConverters(converters -> converters
        .registerDefaults()
        .withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper)))
      .build();

    var adapter = RestClientAdapter.create(restClient);
    return HttpServiceProxyFactory.builderFor(adapter).build().createClient(ApisixAdminClient.class);
  }

  // HttpURLConnection-based factories cannot send PATCH, which the tenant-update path relies on,
  // so the non-TLS case uses the JDK HttpClient factory instead of the shared default.
  private static ClientHttpRequestFactory apisixRequestFactory(ApisixGatewayConfigurationProperties properties) {
    var tls = properties.getTls();
    return tls != null && tls.isEnabled() ? getRequestFactory(tls) : new JdkClientHttpRequestFactory();
  }

  /**
   * Creates {@link ApisixRouteTenantService} bean.
   *
   * @return created {@link ApisixRouteTenantService} bean
   */
  @Bean(name = "folioApisixRouteTenantService")
  @ConditionalOnMissingBean(ApisixRouteTenantService.class)
  public ApisixRouteTenantService apisixRouteTenantService() {
    return new ApisixRouteTenantService();
  }

  /**
   * Creates {@link ApisixGatewayService} bean.
   *
   * @param apisixAdminClient - {@link ApisixAdminClient} bean from spring context
   * @param apisixRouteTenantService - {@link ApisixRouteTenantService} bean from spring context
   * @return created {@link ApisixGatewayService} bean
   */
  @Bean(name = "folioApisixGatewayService")
  @ConditionalOnMissingBean(ApisixGatewayService.class)
  public ApisixGatewayService apisixGatewayService(ApisixAdminClient apisixAdminClient,
    ApisixRouteTenantService apisixRouteTenantService) {
    return new ApisixGatewayService(apisixAdminClient, new ApisixRouteFactory(), apisixRouteTenantService);
  }

  /**
   * Creates {@link ApiGatewayModuleRegistrar} bean.
   *
   * @param apisixGatewayService - {@link ApisixGatewayService} bean from spring context
   * @param objectMapper - {@link ObjectMapper} bean from spring context
   * @param resourceLoader - {@link ResourceLoader} bean from spring context
   * @return created {@link ApiGatewayModuleRegistrar} bean
   */
  @Bean(name = "folioApiGatewayModuleRegistrar")
  @ConditionalOnProperty("application.apigw.register-module")
  public ApiGatewayModuleRegistrar apiGatewayModuleRegistrar(ApisixGatewayService apisixGatewayService,
    ObjectMapper objectMapper, ResourceLoader resourceLoader,
    ApisixGatewayConfigurationProperties properties) {
    var settings = new ModuleRegistrationSettings(properties.getModuleSelfUrl(), properties.getRetries(),
      properties.getConnectTimeout(), properties.getReadTimeout(), properties.getWriteTimeout());
    return new ApiGatewayModuleRegistrar(objectMapper, resourceLoader, apisixGatewayService, settings);
  }
}
