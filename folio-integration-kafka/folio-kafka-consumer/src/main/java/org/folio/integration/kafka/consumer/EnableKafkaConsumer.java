package org.folio.integration.kafka.consumer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerFilteringConfiguration;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerProperties;
import org.folio.integration.kafka.consumer.filter.mmd.configuration.ModuleMetadataConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 *   <li>bind {@link KafkaConsumerProperties} from
 *       {@code application.kafka.consumer.*} configuration properties.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({KafkaConsumerFilteringConfiguration.class, ModuleMetadataConfiguration.class})
@EnableConfigurationProperties(KafkaConsumerProperties.class)
public @interface EnableKafkaConsumer {}
