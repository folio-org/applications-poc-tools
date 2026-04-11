/**
 * Kafka producer infrastructure for FOLIO applications.
 *
 * <p>Provides the {@link org.folio.integration.kafka.producer.EnableKafkaProducer}
 * meta-annotation that activates topic management, the
 * {@link org.folio.integration.kafka.producer.KafkaAdminService} service for programmatic
 * topic creation and deletion, and the {@link org.folio.integration.kafka.producer.KafkaUtils}
 * utility class for building environment- and tenant-scoped topic names.
 * Topic definitions are sourced from
 * {@link org.folio.integration.kafka.producer.KafkaProducerProperties} bound to
 * {@code application.kafka.producer.*}.
 */
package org.folio.integration.kafka.producer;
