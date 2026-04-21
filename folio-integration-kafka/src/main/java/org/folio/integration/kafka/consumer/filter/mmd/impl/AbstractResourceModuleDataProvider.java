package org.folio.integration.kafka.consumer.filter.mmd.impl;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider;

/**
 * Base class for {@link ModuleDataProvider} implementations that read module metadata from
 * a classpath resource embedded in the primary JAR.
 *
 * <p>Delegates the actual loading to the {@link #load()} template method and caches the result
 * (or the failure) after the first invocation using a double-checked-locking pattern to avoid
 * redundant I/O on subsequent calls.
 */
abstract class AbstractResourceModuleDataProvider implements ModuleDataProvider {

  @SuppressWarnings("java:S3077")
  protected volatile Pair<IllegalStateException, ModuleData> data;

  /**
   * Returns the module data, loading it from the primary JAR on the first call.
   */
  @SuppressWarnings("java:S2637")
  public ModuleData getModuleData() {
    if (data == null) {
      synchronized (this) {
        if (data == null) {
          try {
            var loaded = load();
            data = Pair.of(null, loaded);
          } catch (IllegalStateException e) {
            data = Pair.of(e, null);
          }
        }
      }
    }

    if (data.getLeft() != null) {
      throw data.getLeft();
    }
    return data.getRight();
  }

  protected abstract InputStream openResourceStream() throws IOException;

  /**
   * Extracts {@link ModuleData} from the already-opened resource stream.
   *
   * @param resourceStream the open stream to read from; the caller closes it
   * @return the extracted {@link ModuleData}
   * @throws IOException if reading from the stream fails
   */
  protected abstract ModuleData readFromResource(InputStream resourceStream) throws IOException;

  /**
   * Loads the module name and version from the underlying source.
   *
   * @return the loaded module data
   * @throws IllegalStateException if the source is missing, unreadable, or incomplete
   */
  private ModuleData load() {
    try (var resourceStream = openResourceStream()) {
      return readFromResource(resourceStream);
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read module data from resource: " + e.getMessage(), e);
    }
  }
}
