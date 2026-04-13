package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Reads {@link ModuleData} from a {@code META-INF/maven/.../pom.properties} file on the classpath.
 *
 * <p>Uses a {@link PathMatchingResourcePatternResolver} to locate the file with a classpath
 * wildcard pattern under {@code META-INF/maven/}, matching by the artifact ID derived from
 * {@code spring.application.name}.
 * Data is loaded lazily on the first call to {@link #getModuleData()}.
 *
 * <p>Maven places one {@code pom.properties} per artifact at:
 * {@code META-INF/maven/<groupId>/<artifactId>/pom.properties},
 * containing {@code artifactId} and {@code version} properties.
 */
@Slf4j
@RequiredArgsConstructor
public class PomModuleDataProvider extends AbstractResourceModuleDataProvider {

  private static final String POM_PROPERTIES_PATH_PATTERN = "classpath*:META-INF/maven/**/%s/pom.properties";

  private final String applicationName;
  private final PathMatchingResourcePatternResolver resolver;

  @Override
  protected InputStream openResourceStream() throws IOException {
    if (isBlank(applicationName)) {
      throw new IllegalStateException("Application name is blank. "
        + "Cannot determine module data for pom.properties lookup.");
    }

    String pattern = String.format(POM_PROPERTIES_PATH_PATTERN, applicationName);
    Resource[] resources = resolver.getResources(pattern);

    if (ArrayUtils.isEmpty(resources)) {
      throw new IllegalStateException("No pom.properties found for application name: " + applicationName);
    } else if (resources.length > 1) {
      throw new IllegalStateException("Multiple pom.properties found for application name: " + applicationName
        + ". Found at: " + String.join(", ", ArrayUtils.toString(resources)));
    } else {
      return resources[0].getInputStream();
    }
  }

  @Override
  protected ModuleData readFromResource(InputStream resourceStream) throws IOException {
    var props = new Properties();
    props.load(resourceStream);

    return new ModuleData(
      requireProperty(props, "artifactId"),
      requireProperty(props, "version")
    );
  }

  private static String requireProperty(Properties props, String key) {
    var value = props.getProperty(key);
    if (value == null) {
      throw new IllegalStateException("Property '" + key + "' not found in pom.properties");
    }
    return value;
  }
}
