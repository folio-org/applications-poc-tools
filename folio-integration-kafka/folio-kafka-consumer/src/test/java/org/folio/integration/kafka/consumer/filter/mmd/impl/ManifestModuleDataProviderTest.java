package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
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
  void readFromJar_positive_returnsModuleData() throws Exception {
    var provider = new ManifestModuleDataProvider();
    var jarFile = createJarWithManifest(MODULE_NAME, MODULE_VERSION);
    var location = jarFile.toURI().toURL();

    try (var jar = new JarFile(jarFile)) {
      var result = provider.readFromJar(jar, location);

      assertThat(result.name()).isEqualTo(MODULE_NAME);
      assertThat(result.version()).isEqualTo(MODULE_VERSION);
    }
  }

  @Test
  void readFromJar_negative_missingTitleAttribute() throws Exception {
    var provider = new ManifestModuleDataProvider();
    var jarFile = createJarWithManifest(null, MODULE_VERSION);
    var location = jarFile.toURI().toURL();

    try (var jar = new JarFile(jarFile)) {
      assertThatThrownBy(() -> provider.readFromJar(jar, location))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(TITLE_ATTRIBUTE);
    }
  }

  @Test
  void readFromJar_negative_missingVersionAttribute() throws Exception {
    var provider = new ManifestModuleDataProvider();
    var jarFile = createJarWithManifest(MODULE_NAME, null);
    var location = jarFile.toURI().toURL();

    try (var jar = new JarFile(jarFile)) {
      assertThatThrownBy(() -> provider.readFromJar(jar, location))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(VERSION_ATTRIBUTE);
    }
  }

  private static File createJarWithManifest(String title, String version) throws IOException {
    var manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (title != null) {
      manifest.getMainAttributes().putValue(TITLE_ATTRIBUTE, title);
    }
    if (version != null) {
      manifest.getMainAttributes().putValue(VERSION_ATTRIBUTE, version);
    }
    var tempFile = File.createTempFile("test-manifest-", ".jar");
    tempFile.deleteOnExit();
    try (var jos = new JarOutputStream(new FileOutputStream(tempFile), manifest)) {
      // manifest written as first entry by JarOutputStream constructor
    }
    return tempFile;
  }
}
