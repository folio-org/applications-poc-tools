package org.folio.integration.kafka.consumer.filter.mmd.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.jar.JarFile;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Base class for providers that read {@link ModuleData} from the primary JAR.
 *
 * <p>The primary JAR is located by scanning the classpath for a class annotated with
 * {@link SpringBootApplication} and resolving its code source location.
 *
 * <p>Data is loaded lazily: the JAR is not opened until the first call to
 * {@link #getModuleData()}. Subsequent calls return the cached result without
 * re-reading the JAR.
 */
abstract class PrimaryJarModuleDataProvider extends AbstractResourceModuleDataProvider {

  private static final String BASE_PACKAGE = "org.folio";

  protected ModuleData load() {
    var referenceClass = findSpringBootApplicationClass();
    var location = referenceClass.getProtectionDomain().getCodeSource().getLocation();
    try (var jar = new JarFile(new File(location.toURI()))) {
      return readFromJar(jar, location);
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read module data from primary JAR: " + location, e);
    }
  }

  /**
   * Extracts {@link ModuleData} from the already-opened primary JAR.
   *
   * @param jar      the open JAR file to read from
   * @param location the JAR location, for use in error messages
   * @return the extracted {@link ModuleData}
   * @throws IOException if reading from the JAR fails
   */
  protected abstract ModuleData readFromJar(JarFile jar, URL location) throws IOException;

  private static Class<?> findSpringBootApplicationClass() {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(SpringBootApplication.class));
    return scanner.findCandidateComponents(BASE_PACKAGE)
      .stream()
      .map(BeanDefinition::getBeanClassName)
      .filter(Objects::nonNull)
      .map(name -> {
        try {
          return Class.forName(name);
        } catch (ClassNotFoundException e) {
          throw new IllegalStateException("Cannot load @SpringBootApplication class: " + name, e);
        }
      })
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("No @SpringBootApplication class found on classpath"));
  }
}
