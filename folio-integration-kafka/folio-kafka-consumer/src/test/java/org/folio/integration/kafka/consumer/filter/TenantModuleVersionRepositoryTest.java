package org.folio.integration.kafka.consumer.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcOperations;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantModuleVersionRepositoryTest {

  private static final String SCHEMA = "testtenant_mod_foo";
  private static final String MODULE_ID = "mod-foo-2.0.0";
  private static final String TABLE = "\"testtenant_mod_foo\".\"tenant_module_version\"";
  private static final String TABLE_EXISTS_SQL = """
    SELECT EXISTS (
      SELECT 1
      FROM information_schema.tables
      WHERE table_schema = ?
        AND table_name = ?
    )
    """;

  @Mock private JdbcOperations jdbcOperations;

  private TenantModuleVersionRepository repository;

  @BeforeEach
  void setUp() {
    repository = new TenantModuleVersionRepository(jdbcOperations);
  }

  @Test
  void saveModuleVersion_positive_upsertsModuleVersion() {
    repository.saveModuleVersion(SCHEMA, MODULE_ID);

    var sql = """
      INSERT INTO %s (id, module_id)
      VALUES (1, ?)
      ON CONFLICT (id) DO UPDATE
      SET module_id = EXCLUDED.module_id,
          updated_date = CURRENT_TIMESTAMP
      """.formatted(TABLE).strip();

    verify(jdbcOperations).update(sql, MODULE_ID);
  }

  @Test
  void findModuleVersion_moduleVersionExists_returnsModuleVersion() {
    var sql = "SELECT module_id FROM " + TABLE + " WHERE id = 1";
    when(jdbcOperations.queryForList(sql, String.class)).thenReturn(List.of(MODULE_ID));

    var result = repository.findModuleVersion(SCHEMA);

    assertThat(result).contains(MODULE_ID);
  }

  @Test
  void deleteModuleVersion_positive_deletesMarker() {
    repository.deleteModuleVersion(SCHEMA);

    verify(jdbcOperations).update("DELETE FROM " + TABLE);
  }

  @Test
  void markerTableExists_tableExists_returnsTrue() {
    when(jdbcOperations.queryForObject(TABLE_EXISTS_SQL.strip(), Boolean.class, SCHEMA, "tenant_module_version"))
      .thenReturn(true);

    var result = repository.markerTableExists(SCHEMA);

    assertThat(result).isTrue();
  }

  @Test
  void tableExists_businessTableExists_returnsTrue() {
    when(jdbcOperations.queryForObject(TABLE_EXISTS_SQL.strip(), Boolean.class, SCHEMA, "business_table"))
      .thenReturn(true);

    var result = repository.tableExists(SCHEMA, "business_table");

    assertThat(result).isTrue();
  }
}
