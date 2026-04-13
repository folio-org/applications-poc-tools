package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ModulePropertiesModuleDataProviderTest {

  private static final String DEFAULT_LOCATION = "classpath:module.properties";
  private static final String CUSTOM_LOCATION = "classpath:custom/module.properties";
  private static final String MODULE_NAME = "my-module";
  private static final String MODULE_VERSION = "1.2.3";
  private static final String LOAD_FAIL_MSG = "Failed to read module data from resource:";

  @Mock private ResourceLoader resourceLoader;
  @Mock private Resource resource;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(resourceLoader, resource);
  }

  @Test
  void getModuleData_positive_loadsFromDefaultLocation() throws IOException {
    var provider = new ModulePropertiesModuleDataProvider(resourceLoader);

    when(resourceLoader.getResource(DEFAULT_LOCATION)).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(propertiesStream(MODULE_NAME, MODULE_VERSION));

    var result = provider.getModuleData();

    assertThat(result.name()).isEqualTo(MODULE_NAME);
    assertThat(result.version()).isEqualTo(MODULE_VERSION);
  }

  @Test
  void getModuleData_positive_loadsFromCustomLocation() throws IOException {
    var provider = new ModulePropertiesModuleDataProvider(resourceLoader, CUSTOM_LOCATION);

    when(resourceLoader.getResource(CUSTOM_LOCATION)).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(propertiesStream(MODULE_NAME, MODULE_VERSION));

    var result = provider.getModuleData();

    assertThat(result.name()).isEqualTo(MODULE_NAME);
    assertThat(result.version()).isEqualTo(MODULE_VERSION);
  }

  @Test
  void getModuleData_positive_blankLocationFallsBackToDefault() throws IOException {
    var provider = new ModulePropertiesModuleDataProvider(resourceLoader, "  ");

    when(resourceLoader.getResource(DEFAULT_LOCATION)).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(propertiesStream(MODULE_NAME, MODULE_VERSION));

    var result = provider.getModuleData();

    assertThat(result.name()).isEqualTo(MODULE_NAME);
    assertThat(result.version()).isEqualTo(MODULE_VERSION);
  }

  @Test
  void getModuleData_positive_resultIsCachedAfterFirstCall() throws IOException {
    var provider = new ModulePropertiesModuleDataProvider(resourceLoader);

    when(resourceLoader.getResource(DEFAULT_LOCATION)).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(propertiesStream(MODULE_NAME, MODULE_VERSION));

    var first = provider.getModuleData();
    var second = provider.getModuleData();

    assertThat(first).isEqualTo(second);
    verify(resource).getInputStream();
  }

  @Test
  void getModuleData_negative_ioException() throws IOException {
    var provider = new ModulePropertiesModuleDataProvider(resourceLoader, DEFAULT_LOCATION);
    var ioException = new IOException("Resource not found");

    when(resourceLoader.getResource(DEFAULT_LOCATION)).thenReturn(resource);
    when(resource.getInputStream()).thenThrow(ioException);

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(LOAD_FAIL_MSG)
      .hasCause(ioException);
  }

  @Test
  void getModuleData_negative_exceptionIsCached() throws IOException {
    var provider = new ModulePropertiesModuleDataProvider(resourceLoader, DEFAULT_LOCATION);

    when(resourceLoader.getResource(DEFAULT_LOCATION)).thenReturn(resource);
    when(resource.getInputStream()).thenThrow(new IOException("Resource not found"));

    assertThatThrownBy(provider::getModuleData).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(provider::getModuleData).isInstanceOf(IllegalStateException.class);
    verify(resource).getInputStream();
  }

  @Test
  void getModuleData_negative_missingNameProperty() throws IOException {
    var provider = new ModulePropertiesModuleDataProvider(resourceLoader, DEFAULT_LOCATION);

    when(resourceLoader.getResource(DEFAULT_LOCATION)).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(propertiesStream(null, MODULE_VERSION));

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("module.name");
  }

  @Test
  void getModuleData_negative_missingVersionProperty() throws IOException {
    var provider = new ModulePropertiesModuleDataProvider(resourceLoader, DEFAULT_LOCATION);

    when(resourceLoader.getResource(DEFAULT_LOCATION)).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(propertiesStream(MODULE_NAME, null));

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("module.version");
  }

  @Test
  void constructor_negative_nullResourceLoader() {
    assertThatThrownBy(() -> new ModulePropertiesModuleDataProvider(null, DEFAULT_LOCATION))
      .isInstanceOf(NullPointerException.class);
  }

  private static InputStream propertiesStream(String name, String version) {
    var props = new Properties();
    if (name != null) {
      props.setProperty("module.name", name);
    }
    if (version != null) {
      props.setProperty("module.version", version);
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
