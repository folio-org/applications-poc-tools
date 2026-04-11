package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * {@link ModuleDataProvider} that reads module metadata from a {@code module.properties} file
 * on the classpath.
 *
 * <p>The expected property keys are {@code module.name} and {@code module.version}.
 * The default resource location is {@code classpath:module.properties}; an alternative path
 * can be provided at construction time. If the supplied location is blank, the default is used.
 */
@Log4j2
public class ModulePropertiesModuleDataProvider extends AbstractResourceModuleDataProvider {

  private static final String DEFAULT_MODULE_PROPERTIES_FILE = "classpath:module.properties";

  private final ResourceLoader resourceLoader;
  private final String modulePropertiesLocation;

  public ModulePropertiesModuleDataProvider() {
    this(new PathMatchingResourcePatternResolver(), DEFAULT_MODULE_PROPERTIES_FILE);
  }

  public ModulePropertiesModuleDataProvider(ResourceLoader resourceLoader) {
    this(resourceLoader, DEFAULT_MODULE_PROPERTIES_FILE);
  }

  public ModulePropertiesModuleDataProvider(ResourceLoader resourceLoader, String modulePropertiesLocation) {
    Objects.requireNonNull(resourceLoader, "Resource loader is required");
    this.resourceLoader = resourceLoader;

    if (isBlank(modulePropertiesLocation)) {
      log.info("Provided module properties file location is blank. Defaulting to '{}'", DEFAULT_MODULE_PROPERTIES_FILE);
      this.modulePropertiesLocation = DEFAULT_MODULE_PROPERTIES_FILE;
    } else {
      this.modulePropertiesLocation = modulePropertiesLocation;
    }
  }

  @Override
  protected ModuleData load() {
    Properties props = new Properties();
    try (InputStream is = resourceLoader.getResource(modulePropertiesLocation).getInputStream()) {
      props.load(is);

      return new ModuleData(
        requireProperty(props, "module.name", modulePropertiesLocation),
        requireProperty(props, "module.version", modulePropertiesLocation)
      );
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load module properties from resource: " + modulePropertiesLocation, e);
    }
  }

  private static String requireProperty(Properties props, String key, String file) {
    var value = props.getProperty(key);
    if (value == null) {
      throw new IllegalStateException("Property '" + key + "' not found: " + file);
    }
    return value;
  }
}
