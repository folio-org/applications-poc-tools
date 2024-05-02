package org.folio.tools.kong.configuration;

import static org.folio.common.utils.FeignClientTlsUtils.buildTargetFeignClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.folio.tools.kong.client.KongAdminClient;
import org.folio.tools.kong.service.KongGatewayService;
import org.folio.tools.kong.service.KongModuleRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

@Configuration
@Import(FeignClientsConfiguration.class)
@ConditionalOnProperty("application.kong.enabled")
@EnableConfigurationProperties(KongConfigurationProperties.class)
public class KongRegistrarAutoConfiguration {

  /**
   * Creates a {@link org.springframework.cloud.openfeign.FeignClient} for integration with Kong Admin API.
   *
   * @param properties - kong configuration properties with required data
   * @param contract - feign contract
   * @param encoder - feign http body encoder
   * @param decoder - feign http body decoder
   * @return created {@link KongAdminClient} component
   */
  @Bean(name = "folioKongAdminClient")
  @ConditionalOnMissingBean(KongAdminClient.class)
  public KongAdminClient folioKongIntegrationClient(okhttp3.OkHttpClient okHttpClient,
    KongConfigurationProperties properties, Contract contract, Encoder encoder, Decoder decoder) {
    return buildTargetFeignClient(okHttpClient, contract, encoder, decoder, properties.getTls(), properties.getUrl(),
      KongAdminClient.class);
  }

  /**
   * Creates {@link KongGatewayService} bean.
   *
   * @param kongAdminClient - {@link KongAdminClient} bean from spring context.
   * @return created {@link KongGatewayService} bean
   */
  @Bean(name = "folioKongGatewayService")
  @ConditionalOnMissingBean(KongGatewayService.class)
  public KongGatewayService kongGatewayService(KongAdminClient kongAdminClient) {
    return new KongGatewayService(kongAdminClient);
  }

  /**
   * Creates {@link KongModuleRegistrar} bean.
   *
   * @param kongGatewayService - {@link KongGatewayService} bean from spring context
   * @param objectMapper - {@link ObjectMapper} bean from spring context
   * @param resourceLoader - {@link ResourceLoader} bean from spring context
   * @return created {@link KongModuleRegistrar} bean
   */
  @Bean(name = "folioKongModuleRegistrar")
  @ConditionalOnProperty("application.kong.register-module")
  public KongModuleRegistrar kongModuleRegistrar(KongGatewayService kongGatewayService, ObjectMapper objectMapper,
    ResourceLoader resourceLoader, KongConfigurationProperties configurationProperties) {
    return new KongModuleRegistrar(objectMapper, resourceLoader, kongGatewayService, configurationProperties);
  }
}
