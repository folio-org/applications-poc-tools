package org.folio.integration.kafka.consumer.filter.mmd;

@FunctionalInterface
public interface ModuleDataProvider {

  ModuleData getModuleData();
}
