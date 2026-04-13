package org.folio.integration.kafka.consumer.filter.mmd;

/**
 * Strategy interface for loading a module's name and version from a specific source.
 *
 * <p>Implementations throw {@link IllegalStateException} when the underlying source is
 * unavailable or does not contain the required metadata. The
 * {@link org.folio.integration.kafka.consumer.filter.mmd.impl.CompositeModuleDataProvider}
 * chains multiple providers and returns the first successful result.
 */
@FunctionalInterface
public interface ModuleDataProvider {

  /**
   * Loads and returns the module's name and version.
   *
   * @return the module data
   * @throws IllegalStateException if the metadata source is unavailable or incomplete
   */
  ModuleData getModuleData();
}
