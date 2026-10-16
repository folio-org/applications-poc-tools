package org.folio.integration.kafka.consumer.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Optional;
import org.folio.spring.FolioExecutionContext;
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
  private static final String MODULE_ID = "mod-foo-2.0.0";
  private static final String OLD_MODULE_ID = "mod-foo-1.0.0";

  @Mock private TenantModuleVersionRepository repository;
  @Mock private TenantInitializationDetector initializationDetector;
  @Mock private FolioExecutionContext folioExecutionContext;

  private TenantModuleVersionService service;

  @BeforeEach
  void setUp() {
    when(folioExecutionContext.getTenantId()).thenReturn(TENANT);
    service = new TenantModuleVersionService(repository, initializationDetector, folioExecutionContext, MODULE_ID);
  }

  @Test
  void markCurrentVersion_tenantInitialized_recordsCurrentVersion() {
    service.markCurrentVersion();

    verify(repository).saveModuleVersion(MODULE_ID);
  }

  @Test
  void clear_markerTableExists_deletesMarker() {
    when(repository.markerTableExists()).thenReturn(true);

    service.clear();

    verify(repository).deleteModuleVersion();
  }

  @Test
  void isDifferentModuleVersion_matchesCurrentVersion_returnsFalse() {
    mockInitializedTenant();
    when(repository.findModuleVersion()).thenReturn(Optional.of(MODULE_ID));

    var result = service.isDifferentModuleVersion();

    assertThat(result).isFalse();
  }

  @Test
  void isDifferentModuleVersion_doesNotMatchCurrentVersion_returnsTrue() {
    mockInitializedTenant();
    when(repository.findModuleVersion()).thenReturn(Optional.of(OLD_MODULE_ID));

    var result = service.isDifferentModuleVersion();

    assertThat(result).isTrue();
  }

  @Test
  void isDifferentModuleVersion_schemaPredatesMarker_returnsTrue() {
    when(initializationDetector.isInitialized()).thenReturn(true);
    when(repository.markerTableExists()).thenReturn(false);

    var result = service.isDifferentModuleVersion();

    assertThat(result).isTrue();
  }

  @Test
  void isDifferentModuleVersion_tenantNotInitialized_returnsFalse() {
    when(initializationDetector.isInitialized()).thenReturn(false);

    var result = service.isDifferentModuleVersion();

    assertThat(result).isFalse();
  }

  @Test
  void isDifferentModuleVersion_markerRecordMissing_returnsFalse() {
    mockInitializedTenant();
    when(repository.findModuleVersion()).thenReturn(Optional.empty());

    var result = service.isDifferentModuleVersion();

    assertThat(result).isFalse();
  }

  @Test
  void isDifferentModuleVersion_markerTableCached_readsModuleVersionOnEveryCheck() {
    mockInitializedTenant();
    when(repository.findModuleVersion()).thenReturn(Optional.of(MODULE_ID), Optional.of(OLD_MODULE_ID));

    assertThat(service.isDifferentModuleVersion()).isFalse();
    assertThat(service.isDifferentModuleVersion()).isTrue();

    verify(initializationDetector).isInitialized();
    verify(repository).markerTableExists();
    verify(repository, times(2)).findModuleVersion();
  }

  @Test
  void isDifferentModuleVersion_markerTableDroppedAfterCaching_resolvesModuleVersionAgain() {
    mockInitializedTenant();
    when(repository.findModuleVersion())
      .thenReturn(Optional.of(MODULE_ID))
      .thenThrow(new BadSqlGrammarException("query", "SELECT", new SQLException("missing table")));
    when(initializationDetector.isInitialized()).thenReturn(true, false);

    assertThat(service.isDifferentModuleVersion()).isFalse();
    assertThat(service.isDifferentModuleVersion()).isFalse();
  }

  private void mockInitializedTenant() {
    when(initializationDetector.isInitialized()).thenReturn(true);
    when(repository.markerTableExists()).thenReturn(true);
  }
}
