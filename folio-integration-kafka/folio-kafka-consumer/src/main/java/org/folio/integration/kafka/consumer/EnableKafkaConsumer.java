package org.folio.integration.kafka.consumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerFilteringConfiguration;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerPropertiesConfiguration;
import org.folio.integration.kafka.consumer.filter.mmd.configuration.ModuleMetadataConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Meta-annotation that activates the full Kafka consumer infrastructure for a Spring application.
 *
 * <p>Placing this annotation on any {@code @Configuration} class will:
 * <ul>
 *   <li>import {@link KafkaConsumerFilteringConfiguration} to register the
 *       {@code tenantAwareMessageFilter} bean;</li>
 *   <li>import {@link ModuleMetadataConfiguration} to register the
 *       {@link org.folio.integration.kafka.consumer.filter.mmd.ModuleMetadata} bean;</li>
 *   <li>import {@link KafkaConsumerPropertiesConfiguration} to register the
 *       {@code kafkaConsumerProperties} bean bound to
 *       {@code application.kafka.consumer.*}; using an explicit {@code @Bean} method ensures the
 *       bean name is predictable so that SpEL expressions such as
 *       {@code #{kafkaConsumerProperties}} resolve correctly regardless of the consuming
 *       application's component-scan path.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
  KafkaConsumerFilteringConfiguration.class,
  KafkaConsumerPropertiesConfiguration.class,
  ModuleMetadataConfiguration.class
})
public @interface EnableKafkaConsumer {}
