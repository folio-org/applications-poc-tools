package org.folio.integration.kafka;

import static java.util.Optional.ofNullable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.folio.common.configuration.properties.FolioEnvironment;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaUtils {

  /**
   * Returns topic name in the format - `{env}.{topic-name}`.
   *
   * @param name - topic name as {@link String}
   * @return Environment specific topic name as {@link String} object
   */
  public static String getEnvTopicName(String name) {
    return String.format("%s.%s", FolioEnvironment.getFolioEnvName(), name);
  }

  /**
   * Returns topic name in the format - `{env}.{tenant}.{topic-name}`
   *
   * @param name - topic name as {@link String}
   * @param tenantId - tenant id as {@link String}
   * @return topic name as {@link String} object
   */
  public static String getTenantTopicName(String name, String tenantId) {
    return String.format("%s.%s.%s", FolioEnvironment.getFolioEnvName(), tenantId, name);
  }

  /**
   * Creates Kafka topic {@link NewTopic} objects for spring context.
   *
   * @param name - topic name as {@link String}
   * @param numPartitions - number of partitions as {@link Integer}
   * @param replicationFactor - replication factor as {@link Short}
   * @return created {@link NewTopic} object
   */
  public static NewTopic createTopic(String name, Integer numPartitions, Short replicationFactor) {
    return new NewTopic(name, ofNullable(numPartitions), ofNullable(replicationFactor));
  }
}
