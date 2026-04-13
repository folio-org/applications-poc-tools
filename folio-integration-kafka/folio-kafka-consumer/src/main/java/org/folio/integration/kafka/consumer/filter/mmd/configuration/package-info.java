/**
 * Spring {@link org.springframework.context.annotation.Configuration} classes that wire the
 * module-metadata resolution infrastructure into the application context.
 *
 * <p>The central class is
 * {@link org.folio.integration.kafka.consumer.filter.mmd.configuration.ModuleMetadataConfiguration},
 * which registers the ordered chain of
 * {@link org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider} beans and exposes the
 * resolved {@link org.folio.integration.kafka.consumer.filter.mmd.ModuleMetadata} as a singleton
 * bean. It is imported transitively via
 * {@link org.folio.integration.kafka.consumer.EnableKafkaConsumer @EnableKafkaConsumer}.
 */
@NullMarked
package org.folio.integration.kafka.consumer.filter.mmd.configuration;

import org.jspecify.annotations.NullMarked;
