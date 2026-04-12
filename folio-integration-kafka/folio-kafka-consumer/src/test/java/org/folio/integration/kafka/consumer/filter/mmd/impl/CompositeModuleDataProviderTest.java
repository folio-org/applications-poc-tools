package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class CompositeModuleDataProviderTest {

  private static final ModuleData MODULE_DATA = new ModuleData("my-module", "1.0.0");
  private static final String PROVIDER_1_FAIL_MSG = "provider 1 failed";
  private static final String PROVIDER_2_FAIL_MSG = "provider 2 failed";
  private static final String EMPTY_DELEGATES_MSG = "At least one module data provider must be set";

  @Mock private ModuleDataProvider provider1;
  @Mock private ModuleDataProvider provider2;
  @Mock private ModuleDataProvider provider3;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(provider1, provider2, provider3);
  }

  @Test
  void constructor_negative_emptyDelegates() {
    var delegates = Collections.<ModuleDataProvider>emptyList();
    assertThatThrownBy(() -> new CompositeModuleDataProvider(delegates))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(EMPTY_DELEGATES_MSG);
  }

  @Test
  void constructor_negative_nullDelegates() {
    assertThatThrownBy(() -> new CompositeModuleDataProvider(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining(EMPTY_DELEGATES_MSG);
  }

  @Test
  void getModuleData_positive_firstDelegateSucceeds() {
    when(provider1.getModuleData()).thenReturn(MODULE_DATA);
    var composite = new CompositeModuleDataProvider(List.of(provider1, provider2));

    var result = composite.getModuleData();

    assertThat(result).isEqualTo(MODULE_DATA);
    verify(provider2, never()).getModuleData();
  }

  @Test
  void getModuleData_positive_fallsBackToSecondDelegate() {
    when(provider1.getModuleData()).thenThrow(new IllegalStateException(PROVIDER_1_FAIL_MSG));
    when(provider2.getModuleData()).thenReturn(MODULE_DATA);
    var composite = new CompositeModuleDataProvider(List.of(provider1, provider2, provider3));

    var result = composite.getModuleData();

    assertThat(result).isEqualTo(MODULE_DATA);
    verify(provider3, never()).getModuleData();
  }

  @Test
  void getModuleData_positive_fallsBackToLastDelegate() {
    when(provider1.getModuleData()).thenThrow(new IllegalStateException(PROVIDER_1_FAIL_MSG));
    when(provider2.getModuleData()).thenThrow(new IllegalStateException(PROVIDER_2_FAIL_MSG));
    when(provider3.getModuleData()).thenReturn(MODULE_DATA);
    var composite = new CompositeModuleDataProvider(List.of(provider1, provider2, provider3));

    var result = composite.getModuleData();

    assertThat(result).isEqualTo(MODULE_DATA);
  }

  @Test
  void getModuleData_negative_allDelegatesFail() {
    when(provider1.getModuleData()).thenThrow(new IllegalStateException(PROVIDER_1_FAIL_MSG));
    when(provider2.getModuleData()).thenThrow(new IllegalStateException(PROVIDER_2_FAIL_MSG));
    var composite = new CompositeModuleDataProvider(List.of(provider1, provider2));

    assertThatThrownBy(composite::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("No module data can be retrieved");
  }
}
