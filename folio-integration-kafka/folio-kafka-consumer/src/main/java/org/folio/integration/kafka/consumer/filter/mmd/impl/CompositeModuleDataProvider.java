package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider;

@Log4j2
public class CompositeModuleDataProvider implements ModuleDataProvider {

  private final List<ModuleDataProvider> delegates;

  public CompositeModuleDataProvider(List<ModuleDataProvider> delegates) {
    if (isEmpty(delegates)) {
      throw new IllegalArgumentException("At least one module data provider must be set");
    }
    this.delegates = new ArrayList<>(delegates);
  }

  @Override
  public ModuleData getModuleData() {
    for (ModuleDataProvider provider : delegates) {
      try {
        return provider.getModuleData();
      } catch (IllegalStateException e) {
        log.info("ModuleDataProvider {} failed to provide module data with error: {}. Attempting next one in the chain",
          provider.getClass().getSimpleName(), e.getMessage());
      }
    }
    throw new IllegalStateException("No module data can be retrieved. Check configured module data providers");
  }
}
