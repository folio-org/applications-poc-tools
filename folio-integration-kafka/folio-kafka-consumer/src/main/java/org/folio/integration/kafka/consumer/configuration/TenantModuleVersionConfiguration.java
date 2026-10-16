package org.folio.integration.kafka.consumer.configuration;

import org.folio.integration.kafka.consumer.filter.MarkerTableBasedTenantAwareMessageFilter;
import org.folio.integration.kafka.consumer.filter.TenantInitializationDetector;
import org.folio.integration.kafka.consumer.filter.TenantModuleVersionRepository;
import org.folio.integration.kafka.consumer.filter.TenantModuleVersionService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

/** Spring configuration for marker-table-based Kafka tenant filtering. */
@Configuration(proxyBeanMethods = false)
public class TenantModuleVersionConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public TenantModuleVersionRepository tenantModuleVersionRepository(JdbcOperations jdbcOperations) {
    return new TenantModuleVersionRepository(jdbcOperations);
  }

  @Bean
  public TenantModuleVersionService tenantModuleVersionService(TenantModuleVersionRepository repository,
    TenantInitializationDetector initializationDetector,
    FolioExecutionContext context, FolioModuleMetadata folioModuleMetadata) {
    return new TenantModuleVersionService(repository, initializationDetector, context,
      folioModuleMetadata.getModuleId());
  }

  @Bean("markerTableBasedTenantAwareMessageFilter")
  public RecordFilterStrategy<String, Object> markerTableBasedTenantAwareMessageFilter(
    TenantModuleVersionService tenantModuleVersionService, FolioModuleMetadata folioModuleMetadata,
    @Value("${folio.kafka.tenant-filter.enabled:false}") boolean enabled) {
    return new MarkerTableBasedTenantAwareMessageFilter<>(tenantModuleVersionService, folioModuleMetadata, enabled);
  }
}
