package org.folio.integration.kafka.consumer.filter.mmd;

/**
 * Read-only view of a module's identity: its name and version.
 *
 * <p>The default {@link #getModuleId()} method assembles the canonical module identifier
 * as {@code <name>-<version>} (e.g. {@code mod-foo-1.0.0}).
 */
public interface ModuleMetadata {

  /**
   * Returns the module name (e.g. {@code mod-foo}).
   *
   * @return the module name
   */
  String getModuleName();

  /**
   * Returns the module version (e.g. {@code 1.0.0}).
   *
   * @return the module version
   */
  String getModuleVersion();

  /**
   * Returns the canonical module identifier composed as {@code <name>-<version>}.
   *
   * @return the module identifier, e.g. {@code mod-foo-1.0.0}
   */
  default String getModuleId() {
    return getModuleName() + "-" + getModuleVersion();
  }
}
