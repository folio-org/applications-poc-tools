package org.folio.integration.kafka.consumer.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring {@link org.springframework.context.annotation.Configuration Configuration} that registers
 * the {@link KafkaConsumerProperties} bean under the explicit name {@code kafkaConsumerProperties}.
 *
 * <p>Using a {@code @Bean}-annotated factory method (rather than
 * {@code @EnableConfigurationProperties}) guarantees that the bean name is derived from the method
 * name and is therefore stable and predictable. This allows SpEL expressions such as
 * {@code #{kafkaConsumerProperties}} — used in Kafka listener container factory configuration — to
 * resolve correctly even when the consuming application's component-scan path does not include the
 * {@code org.folio.integration.kafka.*} packages.
 *
 * <p>This class is imported by
 * {@link org.folio.integration.kafka.consumer.EnableKafkaConsumer @EnableKafkaConsumer} and
 * should not normally need to be imported directly.
 */
@Configuration
public class KafkaConsumerPropertiesConfiguration {

  /**
   * Creates and binds a {@link KafkaConsumerProperties} instance from the
   * {@code application.kafka.consumer.*} namespace.
   *
   * @return a new, bound {@link KafkaConsumerProperties}
   */
  @Bean
  @ConfigurationProperties("application.kafka.consumer")
  public KafkaConsumerProperties kafkaConsumerProperties() {
    return new KafkaConsumerProperties();
  }
}
