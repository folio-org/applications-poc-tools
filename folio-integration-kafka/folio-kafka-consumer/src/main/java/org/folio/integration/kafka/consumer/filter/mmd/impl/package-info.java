/**
 * Implementations of {@link org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider}
 * that load module metadata from different sources.
 *
 * <p>Available providers in the default resolution priority order:
 * <ol>
 *   <li>{@link org.folio.integration.kafka.consumer.filter.mmd.impl.AppPropertiesModuleDataProvider}
 *       — {@code spring.application.name} / {@code spring.application.version} properties;</li>
 *   <li>{@link org.folio.integration.kafka.consumer.filter.mmd.impl.ManifestModuleDataProvider}
 *       — {@code META-INF/MANIFEST.MF} attributes of the primary JAR;</li>
 *   <li>{@link org.folio.integration.kafka.consumer.filter.mmd.impl.PomModuleDataProvider}
 *       — {@code META-INF/maven/.../pom.properties} embedded in the primary JAR;</li>
 *   <li>{@link org.folio.integration.kafka.consumer.filter.mmd.impl.ModulePropertiesModuleDataProvider}
 *       — a {@code classpath:module.properties} file.</li>
 * </ol>
 * The {@link org.folio.integration.kafka.consumer.filter.mmd.impl.CompositeModuleDataProvider}
 * chains any number of providers, returning the first successful result.
 */
@NullMarked
package org.folio.integration.kafka.consumer.filter.mmd.impl;

import org.jspecify.annotations.NullMarked;
