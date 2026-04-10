package org.folio.integration.kafka.consumer.filter.mmd.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider;

public class AppPropertiesModuleDataProvider implements ModuleDataProvider {

  private final String applicationName;
  private final String applicationVersion;

  public AppPropertiesModuleDataProvider(String applicationName, String applicationVersion) {
    this.applicationName = applicationName;
    this.applicationVersion = applicationVersion;
  }

  @Override
  public ModuleData getModuleData() {
    if (isBlank(applicationName) || isBlank(applicationVersion)) {
      throw new IllegalStateException("Application name or version is blank. Cannot provide module data.");
    } else {
      return new ModuleData(applicationName, applicationVersion);
    }
  }
}
