package org.folio.integration.kafka.consumer.filter.mmd.impl;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider;
import org.jspecify.annotations.Nullable;

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

  protected abstract ModuleData load();
}
