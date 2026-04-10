package org.folio.integration.kafka.consumer.filter.mmd.impl;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;

/**
 * Reads {@link ModuleData} from the {@code META-INF/MANIFEST.MF} of the primary JAR.
 *
 * <p>The primary JAR is located and opened by {@link PrimaryJarModuleDataProvider}.
 * Data is loaded lazily on the first call to {@link #getModuleData()}.
 *
 * <p>Expects the manifest to carry the standard Maven attributes set by
 * {@code maven-jar-plugin} or {@code spring-boot-maven-plugin}:
 * <ul>
 *   <li>{@code Implementation-Title} → module name (artifact ID)</li>
 *   <li>{@code Implementation-Version} → module version</li>
 * </ul>
 */
public class ManifestModuleDataProvider extends PrimaryJarModuleDataProvider {

  private static final String TITLE_ATTRIBUTE = "Implementation-Title";
  private static final String VERSION_ATTRIBUTE = "Implementation-Version";

  @Override
  protected ModuleData readFromJar(JarFile jar, URL location) throws IOException {
    Attributes attributes = jar.getManifest().getMainAttributes();
    return new ModuleData(
      requireAttribute(attributes, TITLE_ATTRIBUTE, location),
      requireAttribute(attributes, VERSION_ATTRIBUTE, location)
    );
  }

  private static String requireAttribute(Attributes attributes, String name, URL location) {
    var value = attributes.getValue(name);
    if (value == null) {
      throw new IllegalStateException("Manifest attribute '" + name + "' not found in: " + location);
    }
    return value;
  }
}
