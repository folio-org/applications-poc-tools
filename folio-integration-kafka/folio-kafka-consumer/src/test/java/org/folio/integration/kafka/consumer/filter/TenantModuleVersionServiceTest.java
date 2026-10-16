package org.folio.integration.kafka.consumer.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.BadSqlGrammarException;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantModuleVersionServiceTest {

  private static final String TENANT = "testtenant";
  private static final String SCHEMA = "testtenant_mod_foo";
  private static final String MODULE_ID = "mod-foo-2.0.0";
  private static final String OLD_MODULE_ID = "mod-foo-1.0.0";

  @Mock private TenantModuleVersionRepository repository;
  @Mock private TenantInitializationDetector initializationDetector;

  private TenantModuleVersionService service;

  @BeforeEach
  void setUp() {
    service = new TenantModuleVersionService(repository, initializationDetector, () -> TENANT,
      ignored -> SCHEMA, MODULE_ID);
  }

  @Test
  void markCurrentVersion_tenantInitialized_recordsCurrentVersion() {
    service.markCurrentVersion();

    verify(repository).saveModuleVersion(SCHEMA, MODULE_ID);
  }

  @Test
  void clear_markerTableExists_deletesMarker() {
    when(repository.markerTableExists(SCHEMA)).thenReturn(true);

    service.clear();

    verify(repository).deleteModuleVersion(SCHEMA);
  }

  @Test
  void isDifferentModuleVersion_matchesCurrentVersion_returnsFalse() {
    mockInitializedTenant();
    when(repository.findModuleVersion(SCHEMA)).thenReturn(Optional.of(MODULE_ID));

    var result = service.isDifferentModuleVersion(TENANT);

    assertThat(result).isFalse();
  }

  @Test
  void isDifferentModuleVersion_doesNotMatchCurrentVersion_returnsTrue() {
    mockInitializedTenant();
    when(repository.findModuleVersion(SCHEMA)).thenReturn(Optional.of(OLD_MODULE_ID));

    var result = service.isDifferentModuleVersion(TENANT);

    assertThat(result).isTrue();
  }

  @Test
  void isDifferentModuleVersion_schemaPredatesMarker_returnsTrue() {
    when(initializationDetector.isInitialized(SCHEMA)).thenReturn(true);
    when(repository.markerTableExists(SCHEMA)).thenReturn(false);

    var result = service.isDifferentModuleVersion(TENANT);

    assertThat(result).isTrue();
  }

  @Test
  void isDifferentModuleVersion_tenantNotInitialized_returnsFalse() {
    when(initializationDetector.isInitialized(SCHEMA)).thenReturn(false);

    var result = service.isDifferentModuleVersion(TENANT);

    assertThat(result).isFalse();
  }

  @Test
  void isDifferentModuleVersion_markerRecordMissing_returnsFalse() {
    mockInitializedTenant();
    when(repository.findModuleVersion(SCHEMA)).thenReturn(Optional.empty());

    var result = service.isDifferentModuleVersion(TENANT);

    assertThat(result).isFalse();
  }

  @Test
  void isDifferentModuleVersion_markerTableCached_readsModuleVersionOnEveryCheck() {
    mockInitializedTenant();
    when(repository.findModuleVersion(SCHEMA)).thenReturn(Optional.of(MODULE_ID), Optional.of(OLD_MODULE_ID));

    assertThat(service.isDifferentModuleVersion(TENANT)).isFalse();
    assertThat(service.isDifferentModuleVersion(TENANT)).isTrue();

    verify(initializationDetector).isInitialized(SCHEMA);
    verify(repository).markerTableExists(SCHEMA);
    verify(repository, times(2)).findModuleVersion(SCHEMA);
  }

  @Test
  void isDifferentModuleVersion_markerTableDroppedAfterCaching_resolvesModuleVersionAgain() {
    mockInitializedTenant();
    when(repository.findModuleVersion(SCHEMA))
      .thenReturn(Optional.of(MODULE_ID))
      .thenThrow(new BadSqlGrammarException("query", "SELECT", new SQLException("missing table")));
    when(initializationDetector.isInitialized(SCHEMA)).thenReturn(true, false);

    assertThat(service.isDifferentModuleVersion(TENANT)).isFalse();
    assertThat(service.isDifferentModuleVersion(TENANT)).isFalse();
  }

  private void mockInitializedTenant() {
    when(initializationDetector.isInitialized(SCHEMA)).thenReturn(true);
    when(repository.markerTableExists(SCHEMA)).thenReturn(true);
  }
}
