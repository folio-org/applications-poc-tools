package org.folio.integration.kafka.consumer.filter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class MarkerTableBasedTenantAwareMessageFilterTest {

  private static final String TENANT = "test-tenant";
  private static final String TOPIC = "test-topic";

  @Mock private TenantModuleVersionService tenantModuleVersionService;

  private MarkerTableBasedTenantAwareMessageFilter<String, Object> filter;

  @BeforeEach
  void setUp() {
    filter = new MarkerTableBasedTenantAwareMessageFilter<>(tenantModuleVersionService, true);
  }

  @Test
  void filter_disabled_acceptsRecord() {
    var disabledFilter = new MarkerTableBasedTenantAwareMessageFilter<String, Object>(
      tenantModuleVersionService, false);

    var result = disabledFilter.filter(consumerRecord(TENANT));

    assertThat(result).isFalse();
    verifyNoInteractions(tenantModuleVersionService);
  }

  @Test
  void filter_moduleVersionMatches_acceptsRecord() {
    var consumerRecord = consumerRecord(TENANT);
    when(tenantModuleVersionService.isDifferentModuleVersion(TENANT)).thenReturn(false);

    var result = filter.filter(consumerRecord);

    assertThat(result).isFalse();
    verify(tenantModuleVersionService).isDifferentModuleVersion(TENANT);
  }

  @Test
  void filter_moduleVersionDiffers_discardsRecord() {
    var consumerRecord = consumerRecord(TENANT);
    when(tenantModuleVersionService.isDifferentModuleVersion(TENANT)).thenReturn(true);

    var result = filter.filter(consumerRecord);

    assertThat(result).isTrue();
    verify(tenantModuleVersionService).isDifferentModuleVersion(TENANT);
  }

  @Test
  void filter_tenantHeaderMissing_acceptsRecord() {
    var consumerRecord = consumerRecord(null);

    var result = filter.filter(consumerRecord);

    assertThat(result).isFalse();
    verifyNoInteractions(tenantModuleVersionService);
  }

  @Test
  void resolveTenant_okapiTenantPresent_prefersOkapiTenant() {
    var consumerRecord = consumerRecord(" okapi-tenant ");
    consumerRecord.headers().add("folio.tenantId", "folio-tenant".getBytes(UTF_8));

    var result = MarkerTableBasedTenantAwareMessageFilter.resolveTenant(consumerRecord);

    assertThat(result).isEqualTo("okapi-tenant");
  }

  @Test
  void resolveTenant_okapiTenantBlank_usesFolioTenant() {
    var consumerRecord = consumerRecord(" ");
    consumerRecord.headers().add("folio.tenantId", " folio-tenant ".getBytes(UTF_8));

    var result = MarkerTableBasedTenantAwareMessageFilter.resolveTenant(consumerRecord);

    assertThat(result).isEqualTo("folio-tenant");
  }

  private static ConsumerRecord<String, Object> consumerRecord(String tenant) {
    var consumerRecord = new ConsumerRecord<String, Object>(TOPIC, 0, 0L, "key", new Object());
    if (tenant != null) {
      consumerRecord.headers().add("x-okapi-tenant", tenant.getBytes(UTF_8));
    }
    return consumerRecord;
  }
}
