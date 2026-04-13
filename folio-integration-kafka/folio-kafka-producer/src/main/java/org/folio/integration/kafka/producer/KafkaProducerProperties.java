package org.folio.integration.kafka.producer;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration properties for the Kafka producer, bound from
 * {@code application.kafka.producer.*}.
 *
 * <p>The main entry point is the {@link #topics} list, which defines the Kafka topics that
 * {@link KafkaTopicConfiguration} will ensure exist on startup.
 *
 * <p>This class is registered as a Spring bean by {@link KafkaProducerPropertiesConfiguration}
 * under the name {@code kafkaProducerProperties}.
 */
@Data
public class KafkaProducerProperties {

  /**
   * Topics properties.
   */
  private List<KafkaTopic> topics;

  /**
   * Descriptor for a single Kafka topic to be created on application startup.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor(staticName = "of")
  public static class KafkaTopic {

    /**
     * Topic name.
     */
    private String name;

    /**
     * Number of partitions for topic.
     */
    private Integer numPartitions;

    /**
     * Replication factor for topic.
     */
    private Short replicationFactor;
  }
}
