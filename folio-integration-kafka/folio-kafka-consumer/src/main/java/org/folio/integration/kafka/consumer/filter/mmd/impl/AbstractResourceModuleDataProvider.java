package org.folio.integration.kafka.consumer.filter.mmd.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider;
import org.jspecify.annotations.Nullable;

/**
 * Base class for {@link ModuleDataProvider} implementations that read module metadata from
 * a classpath resource embedded in the primary JAR.
 *
 * <p>Delegates the actual loading to the {@link #load()} template method and caches the result
 * (or the failure) after the first invocation using a double-checked-locking pattern to avoid
 * redundant I/O on subsequent calls.
 */
abstract class AbstractResourceModuleDataProvider implements ModuleDataProvider {

  protected volatile @Nullable Pair<@Nullable IllegalStateException, @Nullable ModuleData> data;

  /**
   * Returns the module data, loading it from the primary JAR on the first call.
   */
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

  /**
   * Loads the module name and version from the underlying source.
   *
   * @return the loaded module data
   * @throws IllegalStateException if the source is missing, unreadable, or incomplete
   */
  protected abstract ModuleData load();
}
