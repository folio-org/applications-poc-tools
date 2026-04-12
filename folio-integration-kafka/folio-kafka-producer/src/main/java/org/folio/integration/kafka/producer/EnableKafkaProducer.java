package org.folio.integration.kafka.producer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.common.configuration.properties.FolioEnvironment;
import org.springframework.context.annotation.Import;

/**
 * Meta-annotation that activates Kafka producer infrastructure for a Spring application.
 *
 * <p>Placing this annotation on any {@code @Configuration} class will:
 * <ul>
 *   <li>import {@link KafkaTopicConfiguration} to create declared topics on startup;</li>
 *   <li>import {@link FolioEnvironment} to make the FOLIO environment name available;</li>
 *   <li>import {@link KafkaAdminService} to enable programmatic topic management;</li>
 *   <li>import {@link KafkaProducerPropertiesConfiguration} to register the
 *       {@code kafkaProducerProperties} bean bound to
 *       {@code application.kafka.producer.*}; using an explicit {@code @Bean} method ensures the
 *       bean name is predictable so that SpEL expressions such as
 *       {@code #{kafkaProducerProperties}} resolve correctly regardless of the consuming
 *       application's component-scan path.</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
  KafkaTopicConfiguration.class,
  FolioEnvironment.class,
  KafkaAdminService.class,
  KafkaProducerPropertiesConfiguration.class
})
public @interface EnableKafkaProducer {}
