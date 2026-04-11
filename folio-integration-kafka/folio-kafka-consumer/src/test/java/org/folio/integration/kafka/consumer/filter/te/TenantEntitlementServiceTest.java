package org.folio.integration.kafka.consumer.filter.te;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class TenantEntitlementServiceTest {

  private static final String MODULE_ID = "mod-foo-1.0.0";
  private static final Set<String> ENABLED_TENANTS = Set.of("tenant-1", "tenant-2");

  @Mock private TenantEntitlementClient tenantEntitlementClient;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(tenantEntitlementClient);
  }

  @Test
  void constructor_negative_nullModuleId() {
    assertThatThrownBy(() -> new TenantEntitlementService(null, tenantEntitlementClient))
      .isInstanceOf(NullPointerException.class)
      .hasMessageContaining("Module ID must not be null");
  }

  @Test
  void getEnabledTenants_positive_returnsTenants() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(ENABLED_TENANTS);

    var result = service.getEnabledTenants();

    assertThat(result).isEqualTo(ENABLED_TENANTS);
  }

  @Test
  void getEnabledTenants_positive_returnsEmptySet() {
    var service = new TenantEntitlementService(MODULE_ID, tenantEntitlementClient);
    when(tenantEntitlementClient.lookupTenantsByModuleId(MODULE_ID)).thenReturn(Set.of());

    var result = service.getEnabledTenants();

    assertThat(result).isEmpty();
  }
}
