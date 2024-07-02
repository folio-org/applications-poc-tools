package org.folio.integration.kafka;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.integration.kafka.KafkaUtils.getEnvTopicName;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfiguration {

  private final KafkaAdminService kafkaAdminService;
  private final FolioKafkaProperties folioKafkaProperties;

  @PostConstruct
  public void createTopics() {
    if (isEmpty(folioKafkaProperties.getTopics())) {
      log.debug("No topics to create");
      return;
    }

    for (var topic : folioKafkaProperties.getTopics()) {
      var topicName = getEnvTopicName(topic.getName());
      var newTopic = KafkaUtils.createTopic(topicName, topic.getNumPartitions(), topic.getReplicationFactor());
      kafkaAdminService.createTopic(newTopic);
    }
  }
}
