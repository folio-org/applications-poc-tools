package org.folio.integration.kafka.consumer.filter;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcOperations;

/** JDBC repository that stores and retrieves the module version recorded for a tenant schema. */
public class TenantModuleVersionRepository {

  private static final String SAVE_MODULE_VERSION_SQL = """
    INSERT INTO tenant_module_version (id, module_id)
    VALUES (1, ?)
    ON CONFLICT (id) DO UPDATE
    SET module_id = EXCLUDED.module_id,
        updated_date = CURRENT_TIMESTAMP
    """;
  private static final String FIND_MODULE_VERSION_SQL = "SELECT module_id FROM tenant_module_version WHERE id = 1";
  private static final String DELETE_MODULE_VERSION_SQL = "DELETE FROM tenant_module_version";
  private static final String MARKER_TABLE_EXISTS_SQL = """
    SELECT EXISTS (
      SELECT 1
      FROM information_schema.tables
      WHERE table_schema = current_schema()
        AND table_name = 'tenant_module_version'
    )
    """;

  private final JdbcOperations jdbcOperations;

  public TenantModuleVersionRepository(JdbcOperations jdbcOperations) {
    this.jdbcOperations = jdbcOperations;
  }

  public void saveModuleVersion(String moduleId) {
    jdbcOperations.update(SAVE_MODULE_VERSION_SQL.strip(), moduleId);
  }

  public Optional<String> findModuleVersion() {
    var results = jdbcOperations.queryForList(FIND_MODULE_VERSION_SQL, String.class);
    return results.stream().findFirst();
  }

  public void deleteModuleVersion() {
    jdbcOperations.update(DELETE_MODULE_VERSION_SQL);
  }

  public boolean markerTableExists() {
    return Boolean.TRUE.equals(jdbcOperations.queryForObject(MARKER_TABLE_EXISTS_SQL.strip(), Boolean.class));
  }
}
