package org.folio.integration.kafka.consumer.filter;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.springframework.jdbc.core.JdbcOperations;

/** JDBC repository that stores and retrieves the module version recorded for a tenant schema. */
public class TenantModuleVersionRepository {

  private static final String MARKER_TABLE = "tenant_module_version";
  private static final String SAVE_MODULE_VERSION_SQL = """
    INSERT INTO %s (id, module_id)
    VALUES (1, ?)
    ON CONFLICT (id) DO UPDATE
    SET module_id = EXCLUDED.module_id,
        updated_date = CURRENT_TIMESTAMP
    """;
  private static final String FIND_MODULE_VERSION_SQL = "SELECT module_id FROM %s WHERE id = 1";
  private static final String DELETE_MODULE_VERSION_SQL = "DELETE FROM %s";
  private static final String TABLE_EXISTS_SQL = """
    SELECT EXISTS (
      SELECT 1
      FROM information_schema.tables
      WHERE table_schema = ?
        AND table_name = ?
    )
    """;

  private final JdbcOperations jdbcOperations;

  public TenantModuleVersionRepository(JdbcOperations jdbcOperations) {
    this.jdbcOperations = requireNonNull(jdbcOperations);
  }

  @SuppressWarnings("java:S2077") // JDBC cannot bind identifiers; qualifiedTable safely quotes them.
  public void saveModuleVersion(String schema, String moduleId) {
    var table = qualifiedTable(schema, MARKER_TABLE);
    jdbcOperations.update(SAVE_MODULE_VERSION_SQL.formatted(table).strip(), moduleId);
  }

  @SuppressWarnings("java:S2077") // JDBC cannot bind identifiers; qualifiedTable safely quotes them.
  public Optional<String> findModuleVersion(String schema) {
    var sql = FIND_MODULE_VERSION_SQL.formatted(qualifiedTable(schema, MARKER_TABLE));
    var results = jdbcOperations.queryForList(sql, String.class);
    return results.stream().findFirst();
  }

  @SuppressWarnings("java:S2077") // JDBC cannot bind identifiers; qualifiedTable safely quotes them.
  public void deleteModuleVersion(String schema) {
    jdbcOperations.update(DELETE_MODULE_VERSION_SQL.formatted(qualifiedTable(schema, MARKER_TABLE)));
  }

  public boolean markerTableExists(String schema) {
    return tableExists(schema, MARKER_TABLE);
  }

  public boolean tableExists(String schema, String table) {
    return Boolean.TRUE.equals(jdbcOperations.queryForObject(TABLE_EXISTS_SQL.strip(), Boolean.class, schema, table));
  }

  private static String qualifiedTable(String schema, String table) {
    return quoteIdentifier(schema) + "." + quoteIdentifier(table);
  }

  private static String quoteIdentifier(String identifier) {
    return '"' + requireNonNull(identifier).replace("\"", "\"\"") + '"';
  }
}
