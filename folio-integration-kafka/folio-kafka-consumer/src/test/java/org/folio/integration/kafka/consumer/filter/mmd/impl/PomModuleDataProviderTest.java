package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class PomModuleDataProviderTest {

  private static final String MODULE_NAME = "my-module";
  private static final String MODULE_VERSION = "1.0.0";
  private static final String POM_PROPERTIES_PATH = "META-INF/maven/org.test/my-module/pom.properties";
  private static final String ARTIFACT_ID_KEY = "artifactId";
  private static final String VERSION_KEY = "version";

  @Test
  void readFromJar_positive_returnsModuleData() throws Exception {
    var provider = new PomModuleDataProvider();
    var jarFile = createJarWithPomProperties(MODULE_NAME, MODULE_VERSION);
    var location = jarFile.toURI().toURL();

    try (var jar = new JarFile(jarFile)) {
      var result = provider.readFromJar(jar, location);

      assertThat(result.name()).isEqualTo(MODULE_NAME);
      assertThat(result.version()).isEqualTo(MODULE_VERSION);
    }
  }

  @Test
  void readFromJar_negative_noPomPropertiesEntry() throws Exception {
    var provider = new PomModuleDataProvider();
    var jarFile = createEmptyJar();
    var location = jarFile.toURI().toURL();

    try (var jar = new JarFile(jarFile)) {
      assertThatThrownBy(() -> provider.readFromJar(jar, location))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No pom.properties found in JAR");
    }
  }

  @Test
  void readFromJar_negative_missingArtifactId() throws Exception {
    var provider = new PomModuleDataProvider();
    var jarFile = createJarWithPomProperties(null, MODULE_VERSION);
    var location = jarFile.toURI().toURL();

    try (var jar = new JarFile(jarFile)) {
      assertThatThrownBy(() -> provider.readFromJar(jar, location))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(ARTIFACT_ID_KEY);
    }
  }

  @Test
  void readFromJar_negative_missingVersion() throws Exception {
    var provider = new PomModuleDataProvider();
    var jarFile = createJarWithPomProperties(MODULE_NAME, null);
    var location = jarFile.toURI().toURL();

    try (var jar = new JarFile(jarFile)) {
      assertThatThrownBy(() -> provider.readFromJar(jar, location))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(VERSION_KEY);
    }
  }

  private static File createJarWithPomProperties(String artifactId, String version) throws IOException {
    var tempFile = File.createTempFile("test-pom-", ".jar");
    tempFile.deleteOnExit();
    try (var jos = new JarOutputStream(new FileOutputStream(tempFile), baseManifest())) {
      jos.putNextEntry(new JarEntry(POM_PROPERTIES_PATH));
      var props = new Properties();
      if (artifactId != null) {
        props.setProperty(ARTIFACT_ID_KEY, artifactId);
      }
      if (version != null) {
        props.setProperty(VERSION_KEY, version);
      }
      props.store(jos, null);
      jos.closeEntry();
    }
    return tempFile;
  }

  private static File createEmptyJar() throws IOException {
    var tempFile = File.createTempFile("test-empty-", ".jar");
    tempFile.deleteOnExit();
    try (var jos = new JarOutputStream(new FileOutputStream(tempFile), baseManifest())) {
      // no entries
    }
    return tempFile;
  }

  private static Manifest baseManifest() {
    var manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    return manifest;
  }
}
