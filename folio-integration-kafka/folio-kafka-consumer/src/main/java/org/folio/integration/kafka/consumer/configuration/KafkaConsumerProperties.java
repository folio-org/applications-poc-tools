package org.folio.integration.kafka.consumer.configuration;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Application properties for kafka message consumer.
 */
@Data
@ConfigurationProperties("application.kafka.consumer")
public class KafkaConsumerProperties {

  /**
   * Map with settings for application kafka listeners.
   */
  private Map<String, KafkaListenerProperties> listener;

  /**
   * Tenant entitlement filtering configuration.
   */
  @NestedConfigurationProperty
  private KafkaConsumerFiltering filtering = new KafkaConsumerFiltering();

  /**
   * Per-listener configuration keyed by a logical listener name.
   *
   * <p>Each entry can override the Kafka topic pattern and consumer group ID
   * used by a specific listener.
   */
  @Data
  public static class KafkaListenerProperties {

    /**
     * Kafka topic-name pattern for this listener (supports regex).
     */
    private String topicPattern;

    /**
     * Consumer group identifier; overrides the global {@code spring.kafka.consumer.group-id}.
     */
    private String groupId;
  }
}
