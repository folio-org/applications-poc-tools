package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;

@UnitTest
@ExtendWith(MockitoExtension.class)
class BuildPropertiesModuleDataProviderTest {

  private static final String ARTIFACT = "my-module";
  private static final String VERSION = "1.0.0";
  private static final String BLANK_MSG = "artifact or version is blank";

  @Mock private BuildProperties buildProperties;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(buildProperties);
  }

  @Test
  void getModuleData_positive_returnsModuleData() {
    when(buildProperties.getArtifact()).thenReturn(ARTIFACT);
    when(buildProperties.getVersion()).thenReturn(VERSION);
    var provider = new BuildPropertiesModuleDataProvider(buildProperties);

    var result = provider.getModuleData();

    assertThat(result.name()).isEqualTo(ARTIFACT);
    assertThat(result.version()).isEqualTo(VERSION);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void getModuleData_negative_blankArtifact(String blankArtifact) {
    when(buildProperties.getArtifact()).thenReturn(blankArtifact);
    var provider = new BuildPropertiesModuleDataProvider(buildProperties);

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(BLANK_MSG);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void getModuleData_negative_blankVersion(String blankVersion) {
    when(buildProperties.getArtifact()).thenReturn(ARTIFACT);
    when(buildProperties.getVersion()).thenReturn(blankVersion);
    var provider = new BuildPropertiesModuleDataProvider(buildProperties);

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(BLANK_MSG);
  }
}
