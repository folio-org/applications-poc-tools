package org.folio.integration.kafka.producer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring {@link Configuration} that registers the {@link KafkaProducerProperties} bean under
 * the explicit name {@code kafkaProducerProperties}.
 *
 * <p>Using a {@code @Bean}-annotated factory method (rather than
 * {@code @EnableConfigurationProperties}) guarantees that the bean name is derived from the method
 * name and is therefore stable and predictable. This allows SpEL expressions such as
 * {@code #{kafkaProducerProperties}} to resolve correctly even when the consuming application's
 * component-scan path does not include the {@code org.folio.integration.kafka.*} packages.
 *
 * <p>This class is imported by
 * {@link EnableKafkaProducer @EnableKafkaProducer} and should not normally need to be imported
 * directly.
 */
@Configuration
public class KafkaProducerPropertiesConfiguration {

  /**
   * Creates and binds a {@link KafkaProducerProperties} instance from the
   * {@code application.kafka.producer.*} namespace.
   *
   * @return a new, bound {@link KafkaProducerProperties}
   */
  @Bean
  @ConfigurationProperties("application.kafka.producer")
  public KafkaProducerProperties kafkaProducerProperties() {
    return new KafkaProducerProperties();
  }
}
