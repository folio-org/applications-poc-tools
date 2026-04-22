package org.folio.integration.kafka.consumer.it;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.folio.test.TestUtils.asJsonString;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerFilteringConfiguration;
import org.folio.integration.kafka.consumer.configuration.KafkaConsumerPropertiesConfiguration;
import org.folio.integration.kafka.consumer.filter.mmd.configuration.ModuleMetadataConfiguration;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.test.TestUtils;
import org.folio.test.extensions.EnableKafka;
import org.folio.test.extensions.EnableWireMock;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

@IntegrationTest
@EnableKafka
@EnableWireMock
@WireMockStub(scripts = "/wiremock/stubs/get-entitled-tenants.json")
@SpringBootTest(
  classes = {
    KafkaConsumerFilteringConfiguration.class,
    KafkaConsumerPropertiesConfiguration.class,
    ModuleMetadataConfiguration.class,
    KafkaConsumerFilteringIT.TestConfig.class
  },
  webEnvironment = WebEnvironment.NONE)
@TestPropertySource(properties = {
  "spring.application.name=mod-test",
  "spring.application.version=1.0.0",
  "application.kafka.consumer.filtering.tenant-filter.enabled=true",
  "application.kafka.consumer.filtering.tenant-filter.all-tenants-disabled-strategy=SKIP",
  "spring.kafka.consumer.auto-offset-reset=earliest",
  "spring.kafka.consumer.group-id=it-consumer-group"
})
class KafkaConsumerFilteringIT {

  private static final String TOPIC = "test-resource-events";
  private static final String ENTITLED_TENANT = "entitled-tenant";
  private static final String NON_ENTITLED_TENANT = "other-tenant";

  @Autowired private KafkaTemplate<String, String> kafkaTemplate;
  @Autowired private TestEventCollector eventCollector;

  @BeforeEach
  void setUp() {
    eventCollector.clear();
  }

  @Test
  void filter_positive_entitledTenant_messageAccepted() throws Exception {
    kafkaTemplate.send(TOPIC, asJsonString(ResourceEvent.builder().tenant(ENTITLED_TENANT).build())).get();

    var events = eventCollector.getEvents();
    await().atMost(10, SECONDS).untilAsserted(() ->
      assertThat(events).hasSize(1)
    );
    assertThat(events.get(0).value().getTenant()).isEqualTo(ENTITLED_TENANT);
  }

  @Test
  void filter_positive_nonEntitledTenant_messageFiltered() throws Exception {
    kafkaTemplate.send(TOPIC, asJsonString(ResourceEvent.builder().tenant(NON_ENTITLED_TENANT).build())).get();
    kafkaTemplate.send(TOPIC, asJsonString(ResourceEvent.builder().tenant(ENTITLED_TENANT).build())).get();

    var events = eventCollector.getEvents();
    await().atMost(10, SECONDS).untilAsserted(() ->
      assertThat(events).hasSize(1)
    );
    assertThat(events.get(0).value().getTenant()).isEqualTo(ENTITLED_TENANT);
  }

  @org.springframework.kafka.annotation.EnableKafka
  @TestConfiguration
  @EnableConfigurationProperties({KafkaProperties.class})
  static class TestConfig {

    @Bean
    ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    KafkaAdmin kafkaAdmin(KafkaProperties kafkaProperties) {
      return new KafkaAdmin(kafkaProperties.buildAdminProperties(null));
    }

    @Bean
    NewTopic testTopic() {
      return new NewTopic(TOPIC, 1, (short) 1);
    }

    @Bean
    TestEventCollector testEventCollector() {
      return new TestEventCollector();
    }

    @Bean
    DefaultKafkaProducerFactory<String, String> producerFactory(KafkaProperties kafkaProperties) {
      return new DefaultKafkaProducerFactory<>(
        kafkaProperties.buildProducerProperties(null), new StringSerializer(), new StringSerializer());
    }

    @Bean
    KafkaTemplate<String, String> kafkaTemplate(DefaultKafkaProducerFactory<String, String> producerFactory) {
      return new KafkaTemplate<>(producerFactory);
    }

    @Bean("resourceEventContainerFactory")
    ConcurrentKafkaListenerContainerFactory<String, ResourceEvent<Object>> resourceEventContainerFactory(
      KafkaProperties kafkaProperties) {
      var cf = new DefaultKafkaConsumerFactory<>(
        kafkaProperties.buildConsumerProperties(null),
        new StringDeserializer(),
        new ResourceEventDeserializer());

      var factory = new ConcurrentKafkaListenerContainerFactory<String, ResourceEvent<Object>>();
      factory.setConsumerFactory(cf);
      return factory;
    }
  }

  static class TestEventCollector {

    private final List<ConsumerRecord<String, ResourceEvent<Object>>> events = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = TOPIC, containerFactory = "resourceEventContainerFactory",
      filter = "tenantAwareMessageFilter", groupId = "it-filter-group")
    void consume(ConsumerRecord<String, ResourceEvent<Object>> rec) {
      events.add(rec);
    }

    List<ConsumerRecord<String, ResourceEvent<Object>>> getEvents() {
      return Collections.unmodifiableList(events);
    }

    void clear() {
      events.clear();
    }
  }

  static class ResourceEventDeserializer implements Deserializer<ResourceEvent<Object>> {

    @Override
    @SuppressWarnings("unchecked")
    public ResourceEvent<Object> deserialize(String topic, byte[] data) {
      return TestUtils.parse(new String(data, StandardCharsets.UTF_8), ResourceEvent.class);
    }
  }
}
