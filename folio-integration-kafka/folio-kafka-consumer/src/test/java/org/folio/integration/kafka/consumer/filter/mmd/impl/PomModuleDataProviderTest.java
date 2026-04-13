package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PomModuleDataProviderTest {

  private static final String APP_NAME = "my-module";
  private static final String MODULE_VERSION = "1.0.0";

  @Mock private PathMatchingResourcePatternResolver resolver;
  @Mock private Resource pomResource;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(resolver, pomResource);
  }

  @Test
  void getModuleData_positive_returnsModuleData() throws IOException {
    var provider = new PomModuleDataProvider(APP_NAME, resolver);

    when(resolver.getResources(anyString())).thenReturn(new Resource[] {pomResource});
    when(pomResource.getInputStream()).thenReturn(pomPropertiesStream(APP_NAME, MODULE_VERSION));

    var result = provider.getModuleData();

    assertThat(result.name()).isEqualTo(APP_NAME);
    assertThat(result.version()).isEqualTo(MODULE_VERSION);
  }

  @Test
  void getModuleData_negative_noPomPropertiesFound() throws IOException {
    var provider = new PomModuleDataProvider(APP_NAME, resolver);

    when(resolver.getResources(anyString())).thenReturn(new Resource[0]);

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("No pom.properties found for application name: " + APP_NAME);
  }

  @Test
  void getModuleData_negative_multiplePomPropertiesFound() throws IOException {
    var provider = new PomModuleDataProvider(APP_NAME, resolver);

    when(resolver.getResources(anyString())).thenReturn(new Resource[] {pomResource, pomResource});

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Multiple pom.properties found for application name: " + APP_NAME);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void getModuleData_negative_blankApplicationName(String blankName) {
    var provider = new PomModuleDataProvider(blankName, resolver);

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Application name is blank");
  }

  @Test
  void readFromResource_negative_missingArtifactId() {
    var provider = new PomModuleDataProvider(APP_NAME, resolver);

    assertThatThrownBy(() -> provider.readFromResource(pomPropertiesStream(null, MODULE_VERSION)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("artifactId");
  }

  @Test
  void readFromResource_negative_missingVersion() {
    var provider = new PomModuleDataProvider(APP_NAME, resolver);

    assertThatThrownBy(() -> provider.readFromResource(pomPropertiesStream(APP_NAME, null)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("version");
  }

  private static InputStream pomPropertiesStream(String artifactId, String version) {
    var props = new Properties();
    if (artifactId != null) {
      props.setProperty("artifactId", artifactId);
    }
    if (version != null) {
      props.setProperty("version", version);
    }
    var baos = new ByteArrayOutputStream();
    try {
      props.store(baos, null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new ByteArrayInputStream(baos.toByteArray());
  }
}
