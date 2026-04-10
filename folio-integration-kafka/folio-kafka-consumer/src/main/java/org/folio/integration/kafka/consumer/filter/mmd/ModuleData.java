package org.folio.integration.kafka.consumer.filter.mmd;

import static org.apache.commons.lang3.StringUtils.isBlank;

public record ModuleData(String name, String version) {

  public ModuleData {
    if (isBlank(name)) {
      throw new IllegalArgumentException("Module name must not be null or blank");
    }
    if (isBlank(version)) {
      throw new IllegalArgumentException("Module version must not be null or blank");
    }
  }

  public ModuleMetadata asModuleMetadata() {
    return new ModuleMetadata() {
      @Override
      public String getModuleName() {
        return name;
      }

      @Override
      public String getModuleVersion() {
        return version;
      }
    };
  }
}
