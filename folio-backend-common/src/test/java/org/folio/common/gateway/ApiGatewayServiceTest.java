package org.folio.common.gateway;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.folio.common.domain.model.ApplicationScope;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

@UnitTest
class ApiGatewayServiceTest {

  private final ApiGatewayService gateway = mock(ApiGatewayService.class, CALLS_REAL_METHODS);
  private final List<ModuleDescriptor> descriptors = List.of(new ModuleDescriptor().id("mod-test-1.0.0"));

  @ParameterizedTest
  @NullSource
  @EnumSource(value = ApplicationScope.class, names = "REGULAR")
  void scope_positive_legacyProviderAcceptsRegular(ApplicationScope scope) {
    gateway.addRoutes(descriptors, scope);
    gateway.updateRoutes(descriptors, scope);

    verify(gateway).addRoutes(descriptors);
    verify(gateway).updateRoutes(descriptors);
  }

  @Test
  void scope_negative_legacyProviderCannotSilentlyIgnorePlatform() {
    var scope = ApplicationScope.PLATFORM;

    assertThatThrownBy(() -> gateway.addRoutes(descriptors, scope))
      .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> gateway.updateRoutes(descriptors, scope))
      .isInstanceOf(UnsupportedOperationException.class);
    verify(gateway, never()).addRoutes(descriptors);
    verify(gateway, never()).updateRoutes(descriptors);
  }
}
