package org.folio.integration.kafka.producer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.kafka.clients.admin.NewTopic;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KafkaTopicConfigurationTest {

  @InjectMocks private KafkaTopicConfiguration kafkaTopicConfiguration;
  @Mock private KafkaAdminService kafkaAdminService;
  @Mock private KafkaProducerProperties kafkaProducerProperties;

  @Test
  void createTopics_positive() {
    when(kafkaProducerProperties.getTopics()).thenReturn(List.of(
      KafkaProducerProperties.KafkaTopic.of("topic1", 10, null),
      KafkaProducerProperties.KafkaTopic.of("topic2", null, (short) 2),
      KafkaProducerProperties.KafkaTopic.of("topic3", 30, (short) -1)));

    kafkaTopicConfiguration.createTopics();

    verify(kafkaAdminService).createTopic(new NewTopic("folio.topic1", Optional.of(10), Optional.empty()));
    verify(kafkaAdminService).createTopic(new NewTopic("folio.topic2", Optional.empty(), Optional.of((short) 2)));
    verify(kafkaAdminService).createTopic(new NewTopic("folio.topic3", Optional.of(30), Optional.of((short) -1)));
  }

  @Test
  void createTopics_positive_topicsEmpty() {
    when(kafkaProducerProperties.getTopics()).thenReturn(null);

    kafkaTopicConfiguration.createTopics();

    verifyNoInteractions(kafkaAdminService);
  }
}
