package org.folio.integration.kafka.consumer.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.folio.integration.kafka.consumer.filter.TenantModuleVersionRepository;
import org.folio.integration.kafka.consumer.filter.TenantModuleVersionService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
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

  @Test
  void markerTableBasedTenantAwareMessageFilter_positive_enabledWithModuleVersion() {
    contextRunner
      .withPropertyValues("folio.kafka.tenant-filter.enabled=true", "test.module.version=1.0.0")
      .run(context -> assertThat(context)
        .hasNotFailed()
        .hasBean("markerTableBasedTenantAwareMessageFilter"));
  }

  @Test
  void markerTableBasedTenantAwareMessageFilter_negative_enabledWithoutModuleVersion() {
    contextRunner
      .withPropertyValues("folio.kafka.tenant-filter.enabled=true")
      .run(context -> {
        assertThat(context).hasFailed();
        assertThat(context.getStartupFailure()).rootCause()
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Kafka tenant filtering requires a non-blank spring.application.version");
      });
  }

  @Configuration(proxyBeanMethods = false)
  static class JdbcOperationsConfiguration {

    @Bean
    JdbcOperations jdbcOperations() {
      return mock(JdbcOperations.class);
    }

    @Bean
    FolioExecutionContext folioExecutionContext() {
      return mock(FolioExecutionContext.class);
    }

    @Bean
    FolioModuleMetadata folioModuleMetadata(@Value("${test.module.version:}") String moduleVersion) {
      var metadata = mock(FolioModuleMetadata.class);
      when(metadata.getModuleVersion()).thenReturn(
        moduleVersion.isBlank() ? Optional.empty() : Optional.of(moduleVersion));
      when(metadata.getModuleId()).thenReturn(
        moduleVersion.isBlank() ? "mod-test" : "mod-test-" + moduleVersion);
      return metadata;
    }
  }
}
