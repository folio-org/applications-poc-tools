package org.folio.integration.kafka.consumer.configuration;

import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerFiltering.TenantFilter;
import org.folio.integration.kafka.consumer.filter.EnabledTenantMessageFilter;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleMetadata;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementClient;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementService;
import org.folio.integration.kafka.model.ResourceEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

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
    @ConditionalOnMissingBean(name = "tenantAwareMessageFilter")
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
      return consumerRecord -> false;
    }
  }
}
