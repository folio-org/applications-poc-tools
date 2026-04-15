package org.folio.integration.kafka.consumer.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerFiltering.TenantFilter;
import org.folio.integration.kafka.consumer.filter.EnabledTenantMessageFilter;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleMetadata;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementClient;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementService;
import org.folio.integration.kafka.model.ResourceEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring {@link Configuration} that registers the {@code tenantAwareMessageFilter} bean.
 *
 * <p>Two mutually exclusive inner configurations are provided:
 * <ul>
 *   <li>{@link TenantFilterConfiguration} — active when
 *       {@code application.kafka.consumer.filtering.tenant-filter.enabled=true}; connects to the
 *       tenant-entitlement service and registers a real {@link EnabledTenantMessageFilter}.</li>
 *   <li>{@link DisabledTenantFilterConfiguration} — active when the property is {@code false}
 *       or absent; registers a no-op pass-through filter that accepts every record.</li>
 * </ul>
 *
 * <p>Activate this configuration via {@link org.folio.integration.kafka.consumer.EnableKafkaConsumer}
 * or by importing it directly.
 */
@Log4j2
@Configuration
public class KafkaConsumerFilteringConfiguration {

  /**
   * Activates tenant entitlement filtering for Kafka consumers.
   *
   * <p>Enabled when {@code application.kafka.consumer.filtering.tenant-filter.enabled=true}.
   *
   * <p><strong>Required bean:</strong> the consuming application must expose an
   * {@link org.springframework.web.service.invoker.HttpServiceProxyFactory} bean configured
   * with the base URL of the tenant-entitlement service. Without it, context startup will fail.
   */
  @Configuration
  @ConditionalOnProperty(value = "application.kafka.consumer.filtering.tenant-filter.enabled", havingValue = "true")
  @ImportHttpServices(types = TenantEntitlementClient.class, group = "kafka-filter-entitlement-client")
  public static class TenantFilterConfiguration {

    private final ModuleMetadata moduleMetadata;
    private final TenantFilter tenantFilter;

    public TenantFilterConfiguration(ModuleMetadata moduleMetadata, KafkaConsumerProperties kafkaProperties) {
      this.moduleMetadata = moduleMetadata;
      this.tenantFilter = kafkaProperties.getFiltering().getTenantFilter();
    }

    /**
     * Rest Client configuration for the tenant entitlement client.
     *
     * @param okapiUrl the base URL for the tenant entitlement client, provided via application properties;
     *                 must not be blank
     * @param jsonMapper the Jackson JSON mapper to use for message conversion; auto-configured by Spring Boot
     * @param loggingInterceptor logging interceptor from folio-spring-support, if available;
     *                           auto-configured by folio-spring-support when the library is on the classpath
     * @return rest client configurer for kafka-filter-entitlement-client group
     */
    @Bean
    public RestClientHttpServiceGroupConfigurer tenantEntitlementClientGroupConfigurer(
      @Value("${okapi.url}") @NotBlank String okapiUrl, JsonMapper jsonMapper,
      @Qualifier("loggingInterceptor") @Autowired(required = false) ClientHttpRequestInterceptor loggingInterceptor) {

      // the group name should match with the one defined in ImportHttpServices for tenant entitlement client;
      // the configurer will be applied to all clients in that group (in this case, there's only one client)
      return groups -> groups.filterByName("kafka-filter-entitlement-client").forEachClient((group, builder) -> {

        builder
          .baseUrl(okapiUrl) // set base url for the client to a value provided in 'okapi.url' application property
          .configureMessageConverters(converters ->
            converters
              .addCustomConverter(new JacksonJsonHttpMessageConverter(jsonMapper))
              .addCustomConverter(new StringHttpMessageConverter()));

        // utilize logging interceptor from folio-spring-support lib, if available
        if (loggingInterceptor != null) {
          builder
            .bufferContent((uri, httpMethod) -> true)
            .requestInterceptor(loggingInterceptor);
        }

        builder.build();
      });
    }

    @Bean
    public TenantEntitlementService tenantEntitlementService(TenantEntitlementClient tenantEntitlementClient) {
      return new TenantEntitlementService(moduleMetadata.getModuleId(), tenantEntitlementClient);
    }

    @Bean("tenantAwareMessageFilter")
    @ConditionalOnMissingBean(name = "tenantAwareMessageFilter")
    public <K, V extends ResourceEvent<?>> RecordFilterStrategy<K, V> enabledTenantMessageFilter(
      TenantEntitlementService tenantEntitlementService) {
      log.info("Kafka tenant aware message filter enabled: tenantFilter = {}", tenantFilter);
      return new EnabledTenantMessageFilter<>(
        moduleMetadata.getModuleId(),
        tenantEntitlementService,
        tenantFilter.isIgnoreEmptyBatch(),
        tenantFilter.getTenantDisabledStrategy(),
        tenantFilter.getAllTenantsDisabledStrategy()
      );
    }
  }

  /**
   * Registers a no-op {@code tenantAwareMessageFilter} that accepts all records.
   *
   * <p>Active when {@code application.kafka.consumer.filtering.tenant-filter.enabled} is
   * {@code false} or the property is absent.
   */
  @Configuration
  @ConditionalOnProperty(value = "application.kafka.consumer.filtering.tenant-filter.enabled",
    havingValue = "false",
    matchIfMissing = true)
  public static class DisabledTenantFilterConfiguration {

    @Bean("tenantAwareMessageFilter")
    @ConditionalOnMissingBean(name = "tenantAwareMessageFilter")
    public <K, V extends ResourceEvent<?>> RecordFilterStrategy<K, V> disabledTenantMessageFilter() {
      log.info("Kafka tenant aware message filter disabled");
      return consumerRecord -> false;
    }
  }
}
