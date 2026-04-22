package org.folio.integration.kafka.consumer.filter.mmd;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Immutable value record holding a module's name and version.
 *
 * <p>Both components are validated on construction: neither {@code name} nor {@code version}
 * may be {@code null} or blank.
 *
 * @param name    the module name (e.g. {@code mod-foo}); must not be blank
 * @param version the module version (e.g. {@code 1.0.0}); must not be blank
 */
public record ModuleData(String name, String version) {

  public ModuleData {
    if (isBlank(name)) {
      throw new IllegalArgumentException("Module name must not be null or blank");
    }
    if (isBlank(version)) {
      throw new IllegalArgumentException("Module version must not be null or blank");
    }
  }

  /**
   * Returns a {@link ModuleMetadata} view backed by this record's name and version.
   *
   * @return a {@link ModuleMetadata} instance derived from this data
   */
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
