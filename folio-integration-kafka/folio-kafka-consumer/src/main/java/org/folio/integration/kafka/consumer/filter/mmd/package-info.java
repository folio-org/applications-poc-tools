/**
 * Module metadata abstractions used by the Kafka consumer filtering layer.
 *
 * <p>Defines the {@link org.folio.integration.kafka.consumer.filter.mmd.ModuleMetadata} interface,
 * the {@link org.folio.integration.kafka.consumer.filter.mmd.ModuleData} value record, and the
 * {@link org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider} strategy interface
 * for loading a module's identity (name and version) from various sources.
 */
@NullMarked
package org.folio.integration.kafka.consumer.filter.mmd;

import org.jspecify.annotations.NullMarked;
