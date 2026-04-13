package org.folio.integration.kafka.consumer.filter.mmd.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.springframework.core.io.ClassPathResource;

/**
 * Reads {@link ModuleData} from {@code META-INF/MANIFEST.MF} on the classpath.
 *
 * <p>The resource is resolved via {@link org.springframework.core.io.ClassPathResource}.
 * Data is loaded lazily on the first call to {@link #getModuleData()}.
 *
 * <p>Expects the manifest to carry the standard Maven attributes set by
 * {@code maven-jar-plugin} or {@code spring-boot-maven-plugin}:
 * <ul>
 *   <li>{@code Implementation-Title} → module name (artifact ID)</li>
 *   <li>{@code Implementation-Version} → module version</li>
 * </ul>
 */
public class ManifestModuleDataProvider extends AbstractResourceModuleDataProvider {

  private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";
  private static final String TITLE_ATTRIBUTE = "Implementation-Title";
  private static final String VERSION_ATTRIBUTE = "Implementation-Version";

  @Override
  protected InputStream openResourceStream() throws IOException {
    ClassPathResource resource = new ClassPathResource(MANIFEST_PATH);
    if (resource.exists()) {
      return resource.getInputStream();
    } else {
      throw new IOException("Manifest resource not found: " + MANIFEST_PATH);
    }
  }

  @Override
  protected ModuleData readFromResource(InputStream resourceStream) {
    var attributes = getManifestAttributes(resourceStream);

    return new ModuleData(
      requireAttribute(attributes, TITLE_ATTRIBUTE),
      requireAttribute(attributes, VERSION_ATTRIBUTE)
    );
  }

  private static Attributes getManifestAttributes(InputStream resourceStream) {
    try {
      var manifest = new Manifest(resourceStream);
      return manifest.getMainAttributes();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read manifest from primary JAR: " + MANIFEST_PATH, e);
    }
  }

  private static String requireAttribute(Attributes attributes, String name) {
    var value = attributes.getValue(name);
    if (value == null) {
      throw new IllegalStateException("Manifest attribute '" + name + "' not found in: " + MANIFEST_PATH);
    }
    return value;
  }
}
