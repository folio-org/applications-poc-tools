package org.folio.integration.kafka.consumer.configuration;

import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerFiltering.TenantFilter;
import org.folio.integration.kafka.consumer.filter.EnabledTenantMessageFilter;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleMetadata;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementClient;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementService;
import org.folio.integration.kafka.model.ResourceEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

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
  public static class TenantFilterConfiguration {

    private final ModuleMetadata moduleMetadata;
    private final TenantFilter tenantFilter;

    public TenantFilterConfiguration(ModuleMetadata moduleMetadata, KafkaConsumerProperties kafkaProperties) {
      this.moduleMetadata = moduleMetadata;
      this.tenantFilter = kafkaProperties.getFiltering().getTenantFilter();
    }

    @Bean
    public TenantEntitlementClient tenantEntitlementClient(HttpServiceProxyFactory factory) {
      return factory.createClient(TenantEntitlementClient.class);
    }

    @Bean
    public TenantEntitlementService tenantEntitlementService(TenantEntitlementClient tenantEntitlementClient) {
      return new TenantEntitlementService(moduleMetadata.getModuleId(), tenantEntitlementClient);
    }

    @Bean("tenantAwareMessageFilter")
    public <K, V extends ResourceEvent<?>> RecordFilterStrategy<K, V> enabledTenantMessageFilter(
      TenantEntitlementService tenantEntitlementService) {
      return new EnabledTenantMessageFilter<>(
        moduleMetadata.getModuleId(),
        tenantEntitlementService,
        tenantFilter.isIgnoreEmptyBatch(),
        tenantFilter.getTenantDisabledStrategy(),
        tenantFilter.getAllTenantsDisabledStrategy()
      );
    }
  }

  @Configuration
  @ConditionalOnProperty(value = "application.kafka.consumer.filtering.tenant-filter.enabled",
    havingValue = "false",
    matchIfMissing = true)
  public static class DisabledTenantFilterConfiguration {

    @Bean("tenantAwareMessageFilter")
    public <K, V extends ResourceEvent<?>> RecordFilterStrategy<K, V> disabledTenantMessageFilter() {
      return consumerRecord -> false;
    }
  }
}
