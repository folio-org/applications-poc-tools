package org.folio.integration.kafka;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("application.kafka")
public class FolioKafkaProperties {

  /**
   * Topics properties.
   */
  private List<KafkaTopic> topics;

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
