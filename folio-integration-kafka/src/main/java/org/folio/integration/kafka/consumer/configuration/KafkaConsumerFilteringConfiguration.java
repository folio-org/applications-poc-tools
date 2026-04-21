package org.folio.integration.kafka.consumer.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerFiltering.TenantFilter;
import org.folio.integration.kafka.consumer.filter.EnabledTenantMessageFilter;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleMetadata;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementClient;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementService;
import org.folio.integration.kafka.model.TenantAwareEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Configuration that conditionally registers the tenant-aware Kafka message filter beans.
 *
 * <p>When {@code application.kafka.consumer.filtering.tenant-filter.enabled=true}, the
 * {@link TenantFilterConfiguration} inner class creates:
 * <ul>
 *   <li>a {@link TenantEntitlementClient} HTTP interface proxy backed by a
 *       {@link RestClient} pointing at {@code okapi.url};</li>
 *   <li>a {@link TenantEntitlementService} wrapping the client;</li>
 *   <li>an {@link EnabledTenantMessageFilter} registered as the {@code tenantAwareMessageFilter}
 *       bean.</li>
 * </ul>
 *
 * <p>When filtering is disabled (default), a no-op filter is registered instead.
 *
 * <p><b>Spring Boot 3.4 adaptation:</b> The master branch uses {@code @ImportHttpServices} and
 * {@code RestClientHttpServiceGroupConfigurer} (Spring 7.0 / Boot 4.x). This backport uses
 * {@link HttpServiceProxyFactory} with {@link RestClientAdapter}, which is available since
 * Spring 6.1 (Boot 3.2).
 */
@Log4j2
@Configuration
public class KafkaConsumerFilteringConfiguration {

  /**
   * Active when {@code application.kafka.consumer.filtering.tenant-filter.enabled=true}.
   *
   * <p>Requires the {@code okapi.url} property to be set (base URL of the entitlement service).
   */
  @Configuration
  @ConditionalOnProperty(value = "application.kafka.consumer.filtering.tenant-filter.enabled", havingValue = "true")
  public static class TenantFilterConfiguration {

    private final ModuleMetadata moduleMetadata;
    private final TenantFilter tenantFilter;

    public TenantFilterConfiguration(ModuleMetadata moduleMetadata, KafkaConsumerProperties kafkaProperties) {
      this.moduleMetadata = moduleMetadata;
      this.tenantFilter = kafkaProperties.getFiltering().getTenantFilter();
    }

    /**
     * Creates a {@link TenantEntitlementClient} proxy using {@link HttpServiceProxyFactory}.
     *
     * <p>This is the Spring Boot 3.4 equivalent of the {@code @ImportHttpServices} +
     * {@code RestClientHttpServiceGroupConfigurer} approach used on the master branch (Spring 7.0).
     *
     * @param okapiUrl             base URL of the entitlement service
     * @param objectMapper         Jackson object mapper for JSON serialization
     * @param loggingInterceptor   optional logging interceptor from folio-spring-support
     * @return the HTTP interface proxy
     */
    @Bean
    @ConditionalOnMissingBean
    public TenantEntitlementClient tenantEntitlementClient(
      @Value("${okapi.url}") String okapiUrl,
      ObjectMapper objectMapper,
      @Qualifier("loggingInterceptor") @Autowired(required = false)
      ClientHttpRequestInterceptor loggingInterceptor) {

      var builder = RestClient.builder()
        .baseUrl(okapiUrl)
        .messageConverters(converters -> {
          converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
          converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
        });

      if (loggingInterceptor != null) {
        builder.requestInterceptor(loggingInterceptor);
      }

      var factory = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(builder.build()))
        .build();
      return factory.createClient(TenantEntitlementClient.class);
    }

    /**
     * Creates the {@link TenantEntitlementService} bean.
     */
    @Bean
    public TenantEntitlementService tenantEntitlementService(TenantEntitlementClient tenantEntitlementClient) {
      return new TenantEntitlementService(moduleMetadata.getModuleId(), tenantEntitlementClient);
    }

    /**
     * Creates the tenant-aware message filter bean.
     */
    @Bean("tenantAwareMessageFilter")
    @ConditionalOnMissingBean(name = "tenantAwareMessageFilter")
    public <K, V extends TenantAwareEvent> RecordFilterStrategy<K, V> enabledTenantMessageFilter(
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
   * Active when filtering is disabled (default).
   */
  @Configuration
  @ConditionalOnProperty(value = "application.kafka.consumer.filtering.tenant-filter.enabled",
    havingValue = "false",
    matchIfMissing = true)
  public static class DisabledTenantFilterConfiguration {

    /**
     * No-op filter that accepts all records.
     */
    @Bean("tenantAwareMessageFilter")
    @ConditionalOnMissingBean(name = "tenantAwareMessageFilter")
    public <K, V extends TenantAwareEvent> RecordFilterStrategy<K, V> disabledTenantMessageFilter() {
      log.info("Kafka tenant aware message filter disabled");
      return consumerRecord -> false;
    }
  }
}
