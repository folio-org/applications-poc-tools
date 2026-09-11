package org.folio.integration.kafka.consumer.it;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.integration.kafka.model.ResourceResultStatus.FAILURE;
import static org.folio.integration.kafka.model.ResourceResultStatus.SUCCESS;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.integration.kafka.consumer.configuration.EventConfirmationConfiguration;
import org.folio.integration.kafka.consumer.confirmation.ResourceResultEventPublisher;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.integration.kafka.model.ResourceResultEvent;
import org.folio.test.TestUtils;
import org.folio.test.extensions.EnableKafka;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

@IntegrationTest
@EnableKafka
@SpringBootTest(
  classes = {
    EventConfirmationConfiguration.class,
    ResourceResultEventPublisher.class,
    EventConfirmationIT.TestConfig.class
  },
  webEnvironment = WebEnvironment.NONE)
@TestPropertySource(properties = {
  "application.event-confirmation.enabled=true",
  "application.event-confirmation.topic=" + EventConfirmationIT.CONFIRMATION_TOPIC,
  "spring.kafka.consumer.auto-offset-reset=earliest",
  "spring.kafka.consumer.group-id=it-confirmation-group"
})
class EventConfirmationIT {

  static final String CONFIRMATION_TOPIC = "test.mgr-tenant-entitlements.resource-result";

  private static final String EVENT_ID = "5f26fe20-d7bc-11ef-9cd2-0242ac120002";
  private static final String TENANT_ID = "test";
  private static final String RESOURCE_NAME = "Scheduled Job";
  private static final String MODULE_ID = "mod-foo-1.0.0";

  @Autowired private ResourceResultEventPublisher eventPublisher;
  @Autowired private TestResultEventCollector collector;

  @BeforeEach
  void setUp() {
    collector.clear();
  }

  @Test
  void publishSuccessFor_positive_eventArrivesOnConfirmationTopic() {
    eventPublisher.publishSuccessFor(resourceEvent(), MODULE_ID);

    await().atMost(10, SECONDS).untilAsserted(() -> {
      var events = collector.getEvents();
      assertThat(events).hasSize(1);
      var result = events.getFirst().value();
      assertThat(result.getId()).isEqualTo(EVENT_ID);
      assertThat(result.getTenant()).isEqualTo(TENANT_ID);
      assertThat(result.getResourceName()).isEqualTo(RESOURCE_NAME);
      assertThat(result.getModuleId()).isEqualTo(MODULE_ID);
      assertThat(result.getStatus()).isEqualTo(SUCCESS);
      assertThat(result.getDetails()).isNull();
    });
  }

  @Test
  void publishFailureFor_positive_eventArrivesOnConfirmationTopicWithDetails() {
    eventPublisher.publishFailureFor(resourceEvent(), new RuntimeException("processing failed"), MODULE_ID);

    await().atMost(10, SECONDS).untilAsserted(() -> {
      var events = collector.getEvents();
      assertThat(events).hasSize(1);
      var result = events.getFirst().value();
      assertThat(result.getId()).isEqualTo(EVENT_ID);
      assertThat(result.getTenant()).isEqualTo(TENANT_ID);
      assertThat(result.getModuleId()).isEqualTo(MODULE_ID);
      assertThat(result.getStatus()).isEqualTo(FAILURE);
      assertThat(result.getDetails()).contains("processing failed");
    });
  }

  private static ResourceEvent<Object> resourceEvent() {
    return ResourceEvent.<Object>baseBuilder()
      .id(EVENT_ID)
      .tenant(TENANT_ID)
      .resourceName(RESOURCE_NAME)
      .build();
  }

  @org.springframework.kafka.annotation.EnableKafka
  @TestConfiguration
  @EnableConfigurationProperties({KafkaProperties.class})
  static class TestConfig {

    @Bean
    JsonMapper jsonMapper() {
      return new JsonMapper();
    }

    @Bean
    KafkaAdmin kafkaAdmin(KafkaProperties kafkaProperties) {
      return new KafkaAdmin(kafkaProperties.buildAdminProperties());
    }

    @Bean
    NewTopic confirmationTopic() {
      return new NewTopic(CONFIRMATION_TOPIC, 1, (short) 1);
    }

    @Bean
    TestResultEventCollector testResultEventCollector() {
      return new TestResultEventCollector();
    }

    @Bean
    DefaultKafkaProducerFactory<String, ResourceResultEvent> producerFactory(KafkaProperties kafkaProperties) {
      return new DefaultKafkaProducerFactory<>(
        kafkaProperties.buildProducerProperties(), new StringSerializer(), new ResourceResultEventSerializer());
    }

    @Bean
    KafkaTemplate<String, ResourceResultEvent> kafkaTemplate(
      DefaultKafkaProducerFactory<String, ResourceResultEvent> producerFactory) {
      return new KafkaTemplate<>(producerFactory);
    }

    @Bean("resultEventContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, ResourceResultEvent> resultEventContainerFactory(
      KafkaProperties kafkaProperties) {
      var cf = new DefaultKafkaConsumerFactory<>(
        kafkaProperties.buildConsumerProperties(),
        new StringDeserializer(),
        new ResultEventDeserializer());
      var factory = new ConcurrentKafkaListenerContainerFactory<String, ResourceResultEvent>();
      factory.setConsumerFactory(cf);
      return factory;
    }
  }

  static class TestResultEventCollector {

    private final List<ConsumerRecord<String, ResourceResultEvent>> events = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = CONFIRMATION_TOPIC, containerFactory = "resultEventContainerFactory",
      groupId = "it-confirmation-collector")
    void consume(ConsumerRecord<String, ResourceResultEvent> rec) {
      events.add(rec);
    }

    List<ConsumerRecord<String, ResourceResultEvent>> getEvents() {
      return Collections.unmodifiableList(events);
    }

    void clear() {
      events.clear();
    }
  }

  static class ResourceResultEventSerializer implements Serializer<ResourceResultEvent> {

    @Override
    public byte[] serialize(String topic, ResourceResultEvent data) {
      return data == null ? null : TestUtils.asJsonString(data).getBytes(StandardCharsets.UTF_8);
    }
  }

  static class ResultEventDeserializer implements Deserializer<ResourceResultEvent> {

    @Override
    public ResourceResultEvent deserialize(String topic, byte[] data) {
      return TestUtils.parse(new String(data, StandardCharsets.UTF_8), ResourceResultEvent.class);
    }
  }
}
