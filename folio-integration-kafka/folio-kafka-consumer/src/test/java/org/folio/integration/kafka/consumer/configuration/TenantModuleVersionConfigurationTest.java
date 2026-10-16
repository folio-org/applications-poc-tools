package org.folio.integration.kafka.consumer.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.folio.integration.kafka.consumer.filter.TenantInitializationDetector;
import org.folio.integration.kafka.consumer.filter.TenantModuleVersionRepository;
import org.folio.integration.kafka.consumer.filter.TenantModuleVersionService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;

@UnitTest
class TenantModuleVersionConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withUserConfiguration(TenantModuleVersionConfiguration.class, JdbcOperationsConfiguration.class);

  @Test
  void tenantModuleVersionRepository_positive_registered() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(TenantModuleVersionRepository.class);
      assertThat(context).hasSingleBean(TenantModuleVersionService.class);
      assertThat(context).hasBean("markerTableBasedTenantAwareMessageFilter");
    });
  }

  @Configuration(proxyBeanMethods = false)
  static class JdbcOperationsConfiguration {

    @Bean
    JdbcOperations jdbcOperations() {
      return mock(JdbcOperations.class);
    }

    @Bean
    TenantInitializationDetector tenantInitializationDetector() {
      return () -> false;
    }

    @Bean
    FolioExecutionContext folioExecutionContext() {
      return mock(FolioExecutionContext.class);
    }

    @Bean
    FolioModuleMetadata folioModuleMetadata() {
      var metadata = mock(FolioModuleMetadata.class);
      when(metadata.getModuleId()).thenReturn("mod-test-1.0.0");
      return metadata;
    }
  }
}
