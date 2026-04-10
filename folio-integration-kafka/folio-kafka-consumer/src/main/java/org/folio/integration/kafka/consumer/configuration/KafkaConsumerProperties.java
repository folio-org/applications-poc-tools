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

  @NestedConfigurationProperty
  private KafkaConsumerFiltering filtering = new KafkaConsumerFiltering();

  @Data
  public static class KafkaListenerProperties {

    private String topicPattern;
    private String groupId;
  }
}
