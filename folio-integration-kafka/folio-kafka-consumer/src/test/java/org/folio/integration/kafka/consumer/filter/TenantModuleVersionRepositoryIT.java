package org.folio.integration.kafka.consumer.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.folio.common.utils.OkapiHeaders;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.config.DataSourceFolioWrapper;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.spring.scope.FolioExecutionScopeConfig;
import org.folio.test.extensions.EnablePostgres;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@IntegrationTest
@EnablePostgres
@SpringBootTest(classes = {
  FolioExecutionScopeConfig.class,
  TenantModuleVersionRepositoryIT.TestConfig.class
}, webEnvironment = WebEnvironment.NONE)
class TenantModuleVersionRepositoryIT {

  private static final String TENANT = "repository_tenant";
  private static final String MODULE_ID = "mod-foo-2.0.0";
  private static final String UPDATED_MODULE_ID = "mod-foo-2.1.0";

  @Autowired private JdbcOperations jdbcOperations;
  @Autowired private TenantModuleVersionRepository repository;
  @Autowired private FolioModuleMetadata folioModuleMetadata;

  @BeforeEach
  void setUp() {
    var schema = folioModuleMetadata.getDBSchemaName(TENANT);
    jdbcOperations.execute("DROP SCHEMA IF EXISTS \"%s\" CASCADE".formatted(schema));
    jdbcOperations.execute("CREATE SCHEMA \"%s\"".formatted(schema));
  }

  @Test
  void markerTableExists_tableCreated_returnsExpectedState() {
    withTenant(() -> assertThat(repository.markerTableExists()).isFalse());

    createMarkerTable();

    withTenant(() -> assertThat(repository.markerTableExists()).isTrue());
  }

  @Test
  void moduleVersionLifecycle_insertUpdateFindDelete_succeeds() {
    createMarkerTable();

    withTenant(() -> {
      assertThat(repository.findModuleVersion()).isEmpty();

      repository.saveModuleVersion(MODULE_ID);
      assertThat(repository.findModuleVersion()).contains(MODULE_ID);

      repository.saveModuleVersion(UPDATED_MODULE_ID);
      assertThat(repository.findModuleVersion()).contains(UPDATED_MODULE_ID);

      repository.deleteModuleVersion();
      assertThat(repository.findModuleVersion()).isEmpty();
    });
  }

  private void createMarkerTable() {
    var schema = folioModuleMetadata.getDBSchemaName(TENANT);
    jdbcOperations.execute("""
      CREATE TABLE "%s".tenant_module_version (
        id INTEGER PRIMARY KEY,
        module_id TEXT NOT NULL,
        updated_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
      )
      """.formatted(schema));
  }

  private void withTenant(Runnable operation) {
    var headers = Map.<String, Collection<String>>of(OkapiHeaders.TENANT, List.of(TENANT));
    try (var ignored = new FolioExecutionContextSetter(folioModuleMetadata, headers)) {
      operation.run();
    }
  }

  @TestConfiguration
  static class TestConfig {

    @Bean
    DataSource dataSource(FolioExecutionContext folioExecutionContext) {
      var dataSource = new DriverManagerDataSource(
        System.getProperty("spring.datasource.url"), "folio_admin", "qwerty123");
      return new DataSourceFolioWrapper(dataSource, folioExecutionContext);
    }

    @Bean
    JdbcOperations jdbcOperations(DataSource dataSource) {
      return new JdbcTemplate(dataSource);
    }

    @Bean
    TenantModuleVersionRepository tenantModuleVersionRepository(JdbcOperations jdbcOperations) {
      return new TenantModuleVersionRepository(jdbcOperations);
    }

    @Bean
    FolioModuleMetadata folioModuleMetadata() {
      return new FolioModuleMetadata() {
        @Override
        public String getModuleName() {
          return "mod-test";
        }

        @Override
        public Optional<String> getModuleVersion() {
          return Optional.of("2.0.0");
        }

        @Override
        public String getDBSchemaName(String tenantId) {
          return tenantId;
        }
      };
    }
  }
}
