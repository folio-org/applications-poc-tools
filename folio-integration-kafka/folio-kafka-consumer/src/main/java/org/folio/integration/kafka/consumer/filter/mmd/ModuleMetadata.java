package org.folio.integration.kafka.consumer.filter.mmd;

public interface ModuleMetadata {

  String getModuleName();

  String getModuleVersion();

  default String getModuleId() {
    return getModuleName() + "-" + getModuleVersion();
  }
}
