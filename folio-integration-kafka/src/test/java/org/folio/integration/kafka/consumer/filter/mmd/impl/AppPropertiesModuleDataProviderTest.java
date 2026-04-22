package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@UnitTest
class AppPropertiesModuleDataProviderTest {

  private static final String MODULE_NAME = "my-module";
  private static final String MODULE_VERSION = "1.0.0";
  private static final String BLANK_MSG = "Application name or version is blank";

  @Test
  void getModuleData_positive_returnsModuleData() {
    var provider = new AppPropertiesModuleDataProvider(MODULE_NAME, MODULE_VERSION);

    var result = provider.getModuleData();

    assertThat(result.name()).isEqualTo(MODULE_NAME);
    assertThat(result.version()).isEqualTo(MODULE_VERSION);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void getModuleData_negative_blankName(String blankName) {
    var provider = new AppPropertiesModuleDataProvider(blankName, MODULE_VERSION);

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(BLANK_MSG);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void getModuleData_negative_blankVersion(String blankVersion) {
    var provider = new AppPropertiesModuleDataProvider(MODULE_NAME, blankVersion);

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(BLANK_MSG);
  }
}
