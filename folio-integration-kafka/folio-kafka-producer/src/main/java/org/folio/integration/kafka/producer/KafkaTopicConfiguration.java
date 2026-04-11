package org.folio.integration.kafka.producer;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.folio.integration.kafka.producer.KafkaUtils.getEnvTopicName;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;

/**
 * Spring {@link Configuration} that creates the Kafka topics declared in
 * {@link KafkaProducerProperties} during application startup.
 *
 * <p>Topic names are prefixed with the FOLIO environment name via
 * {@link KafkaUtils#getEnvTopicName(String)} before being passed to
 * {@link KafkaAdminService#createTopic(org.apache.kafka.clients.admin.NewTopic)}.
 */
@Log4j2
@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfiguration {

  private final KafkaAdminService kafkaAdminService;
  private final KafkaProducerProperties kafkaProducerProperties;

  /**
   * Creates all topics declared in {@link KafkaProducerProperties#getTopics()} after the
   * Spring context has been fully initialised.
   */
  @PostConstruct
  public void createTopics() {
    for (var topic : emptyIfNull(kafkaProducerProperties.getTopics())) {
      var topicName = getEnvTopicName(topic.getName());
      var newTopic = KafkaUtils.createTopic(topicName, topic.getNumPartitions(), topic.getReplicationFactor());
      kafkaAdminService.createTopic(newTopic);
    }
  }
}
