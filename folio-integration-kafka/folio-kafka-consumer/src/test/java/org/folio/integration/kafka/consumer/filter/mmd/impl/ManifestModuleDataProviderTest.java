package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class ManifestModuleDataProviderTest {

  private static final String MODULE_NAME = "my-module";
  private static final String MODULE_VERSION = "1.0.0";
  private static final String TITLE_ATTRIBUTE = "Implementation-Title";
  private static final String VERSION_ATTRIBUTE = "Implementation-Version";

  @Test
  void readFromResource_positive_returnsModuleData() {
    var provider = new ManifestModuleDataProvider();

    var result = provider.readFromResource(manifestStream(MODULE_NAME, MODULE_VERSION));

    assertThat(result.name()).isEqualTo(MODULE_NAME);
    assertThat(result.version()).isEqualTo(MODULE_VERSION);
  }

  @Test
  void readFromResource_negative_missingTitleAttribute() {
    var provider = new ManifestModuleDataProvider();

    assertThatThrownBy(() -> provider.readFromResource(manifestStream(null, MODULE_VERSION)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(TITLE_ATTRIBUTE);
  }

  @Test
  void readFromResource_negative_missingVersionAttribute() {
    var provider = new ManifestModuleDataProvider();

    assertThatThrownBy(() -> provider.readFromResource(manifestStream(MODULE_NAME, null)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining(VERSION_ATTRIBUTE);
  }

  @Test
  void getModuleData_negative_ioException() {
    var provider = new ManifestModuleDataProvider() {
      @Override
      protected InputStream openResourceStream() throws IOException {
        throw new IOException("Manifest not found");
      }
    };

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Failed to read module data from resource");
  }

  @Test
  void getModuleData_negative_manifestNotFound() {
    var provider = new ManifestModuleDataProvider() {
      @Override
      protected InputStream openResourceStream() throws IOException {
        throw new IOException("Manifest resource not found: META-INF/MANIFEST.MF");
      }
    };

    assertThatThrownBy(provider::getModuleData)
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Manifest resource not found");
  }

  private static InputStream manifestStream(String title, String version) {
    var manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (title != null) {
      manifest.getMainAttributes().putValue(TITLE_ATTRIBUTE, title);
    }
    if (version != null) {
      manifest.getMainAttributes().putValue(VERSION_ATTRIBUTE, version);
    }
    var baos = new ByteArrayOutputStream();
    try {
      manifest.write(baos);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new ByteArrayInputStream(baos.toByteArray());
  }
}
