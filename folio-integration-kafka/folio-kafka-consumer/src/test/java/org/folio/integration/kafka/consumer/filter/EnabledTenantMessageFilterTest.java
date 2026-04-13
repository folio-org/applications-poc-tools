package org.folio.integration.kafka.consumer.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy.ACCEPT;
import static org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy.FAIL;
import static org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy.SKIP;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.integration.kafka.consumer.filter.te.TenantEntitlementService;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class EnabledTenantMessageFilterTest {

  private static final String MODULE_ID = "mod-foo-1.0.0";
  private static final String TENANT = "test-tenant";
  private static final String OTHER_TENANT = "other-tenant";
  private static final Set<String> ENABLED_TENANTS = Set.of(TENANT);
  private static final String BLANK_MODULE_ID_MSG = "Module ID must not be blank";

  @Mock private TenantEntitlementService tenantEntitlementService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(tenantEntitlementService);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void constructor_negative_blankModuleId(String blankModuleId) {
    assertThatThrownBy(
      () -> new EnabledTenantMessageFilter<>(blankModuleId, tenantEntitlementService, false, SKIP, SKIP))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(BLANK_MODULE_ID_MSG);
  }

  @Test
  void constructor_negative_nullTenantDisabledStrategy() {
    assertThatThrownBy(
      () -> new EnabledTenantMessageFilter<>(MODULE_ID, tenantEntitlementService, false, null, SKIP))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  void constructor_negative_nullAllTenantsDisabledStrategy() {
    assertThatThrownBy(
      () -> new EnabledTenantMessageFilter<>(MODULE_ID, tenantEntitlementService, false, SKIP, null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  void filter_positive_enabledTenant_returnsAccepted() {
    var filter = createFilter(false, SKIP, SKIP);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);

    var result = filter.filter(consumerRecord("key-1", TENANT));

    assertThat(result).isFalse();
  }

  @Test
  void filter_positive_disabledTenant_acceptStrategy_returnsAccepted() {
    var filter = createFilter(false, ACCEPT, SKIP);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);

    var result = filter.filter(consumerRecord("key-1", OTHER_TENANT));

    assertThat(result).isFalse();
  }

  @Test
  void filter_positive_disabledTenant_skipStrategy_returnsFiltered() {
    var filter = createFilter(false, SKIP, ACCEPT);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);

    var result = filter.filter(consumerRecord("key-1", OTHER_TENANT));

    assertThat(result).isTrue();
  }

  @Test
  void filter_negative_disabledTenant_failStrategy_throwsTenantIsDisabledException() {
    var filter = createFilter(false, FAIL, SKIP);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(ENABLED_TENANTS);
    var rec = consumerRecord("key-1", OTHER_TENANT);

    assertThatThrownBy(() -> filter.filter(rec))
      .isInstanceOf(TenantIsDisabledException.class)
      .hasMessageContaining(OTHER_TENANT)
      .hasMessageContaining(MODULE_ID);
  }

  @Test
  void filter_positive_allTenantsDisabled_acceptStrategy_returnsAccepted() {
    var filter = createFilter(false, SKIP, ACCEPT);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(Set.of());

    var result = filter.filter(consumerRecord("key-1", TENANT));

    assertThat(result).isFalse();
  }

  @Test
  void filter_positive_allTenantsDisabled_skipStrategy_returnsFiltered() {
    var filter = createFilter(false, ACCEPT, SKIP);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(Set.of());

    var result = filter.filter(consumerRecord("key-1", TENANT));

    assertThat(result).isTrue();
  }

  @Test
  void filter_negative_allTenantsDisabled_failStrategy_throwsTenantsAreDisabledException() {
    var filter = createFilter(false, SKIP, FAIL);
    when(tenantEntitlementService.getEnabledTenants()).thenReturn(Set.of());
    var rec = consumerRecord("key-1", TENANT);

    assertThatThrownBy(() -> filter.filter(rec))
      .isInstanceOf(TenantsAreDisabledException.class)
      .hasMessageContaining(MODULE_ID);
  }

  @Test
  void ignoreEmptyBatch_positive_returnsTrue() {
    var filter = createFilter(true, SKIP, SKIP);

    assertThat(filter.ignoreEmptyBatch()).isTrue();
  }

  @Test
  void ignoreEmptyBatch_positive_returnsFalse() {
    var filter = createFilter(false, SKIP, SKIP);

    assertThat(filter.ignoreEmptyBatch()).isFalse();
  }

  private EnabledTenantMessageFilter<String, ResourceEvent<Object>> createFilter(
    boolean ignoreEmptyBatch, DisabledTenantStrategy tenantStrategy, DisabledTenantStrategy allTenantsStrategy) {
    return new EnabledTenantMessageFilter<>(MODULE_ID, tenantEntitlementService,
      ignoreEmptyBatch, tenantStrategy, allTenantsStrategy);
  }

  private static ConsumerRecord<String, ResourceEvent<Object>> consumerRecord(String key, String tenant) {
    var event = ResourceEvent.builder().tenant(tenant).build();
    return new ConsumerRecord<>("test-topic", 0, 0L, key, event);
  }
}
