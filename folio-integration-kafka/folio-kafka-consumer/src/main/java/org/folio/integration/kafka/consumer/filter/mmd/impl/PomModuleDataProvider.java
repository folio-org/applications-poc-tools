package org.folio.integration.kafka.consumer.filter.mmd.impl;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.jar.JarFile;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;

/**
 * Reads {@link ModuleData} from the {@code META-INF/maven/.../pom.properties} file
 * embedded in the primary JAR.
 *
 * <p>The primary JAR is located and opened by {@link PrimaryJarModuleDataProvider}.
 * Data is loaded lazily on the first call to {@link #getModuleData()}.
 *
 * <p>Maven places one {@code pom.properties} per artifact at:
 * {@code META-INF/maven/<groupId>/<artifactId>/pom.properties},
 * containing {@code artifactId} and {@code version} properties.
 */
public class PomModuleDataProvider extends PrimaryJarModuleDataProvider {

  private static final String POM_PROPERTIES_PREFIX = "META-INF/maven/";
  private static final String POM_PROPERTIES_SUFFIX = "/pom.properties";

  @Override
  protected ModuleData readFromJar(JarFile jar, URL location) throws IOException {
    var entry = jar.stream()
      .filter(e -> e.getName().startsWith(POM_PROPERTIES_PREFIX)
        && e.getName().endsWith(POM_PROPERTIES_SUFFIX))
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No pom.properties found in JAR: " + location));

    var props = new Properties();
    try (var inputStream = jar.getInputStream(entry)) {
      props.load(inputStream);
    }

    return new ModuleData(
      requireProperty(props, "artifactId", location),
      requireProperty(props, "version", location)
    );
  }

  private static String requireProperty(Properties props, String key, URL location) {
    var value = props.getProperty(key);
    if (value == null) {
      throw new IllegalStateException("Property '" + key + "' not found in pom.properties in: " + location);
    }
    return value;
  }
}
