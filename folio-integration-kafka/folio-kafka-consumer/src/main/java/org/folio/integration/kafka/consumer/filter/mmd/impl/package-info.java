/**
 * Implementations of {@link org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider}
 * that load module metadata from different sources.
 *
 * <p>Available providers in the default resolution priority order:
 * <ol>
 *   <li>{@link org.folio.integration.kafka.consumer.filter.mmd.impl.AppPropertiesModuleDataProvider}
 *       — {@code spring.application.name} / {@code spring.application.version} properties;</li>
 *   <li>{@link org.folio.integration.kafka.consumer.filter.mmd.impl.BuildPropertiesModuleDataProvider}
 *       — {@code build.artifact} / {@code build.version} from Spring Boot's
 *       {@code META-INF/build-info.properties} (registered only when a
 *       {@link org.springframework.boot.info.BuildProperties} bean is present);</li>
 *   <li>{@link org.folio.integration.kafka.consumer.filter.mmd.impl.ManifestModuleDataProvider}
 *       — {@code Implementation-Title} / {@code Implementation-Version} attributes in
 *       {@code META-INF/MANIFEST.MF} on the classpath;</li>
 *   <li>{@link org.folio.integration.kafka.consumer.filter.mmd.impl.PomModuleDataProvider}
 *       — {@code artifactId} / {@code version} in {@code META-INF/maven/.../pom.properties}
 *       located via a classpath wildcard pattern;</li>
 *   <li>{@link org.folio.integration.kafka.consumer.filter.mmd.impl.ModulePropertiesModuleDataProvider}
 *       — {@code module.name} / {@code module.version} in a {@code classpath:module.properties}
 *       file.</li>
 * </ol>
 * The {@link org.folio.integration.kafka.consumer.filter.mmd.impl.CompositeModuleDataProvider}
 * chains any number of providers, returning the first successful result.
 * The {@link org.folio.integration.kafka.consumer.filter.mmd.impl.AbstractResourceModuleDataProvider}
 * base class provides lazy loading with caching for providers that read from classpath resources.
 */
@NullMarked
package org.folio.integration.kafka.consumer.filter.mmd.impl;

import org.jspecify.annotations.NullMarked;
