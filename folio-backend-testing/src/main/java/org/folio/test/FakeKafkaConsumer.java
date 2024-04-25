package org.folio.test;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.folio.test.TestUtils.parse;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class FakeKafkaConsumer {

  private static final Map<String, List<ConsumerRecord<String, String>>> EVENTS = new ConcurrentHashMap<>();
  private static final Map<String, KafkaMessageListenerContainer<String, String>> CONTAINERS =
    new ConcurrentHashMap<>();

  @Autowired private KafkaProperties kafkaProperties;

  public <T> void registerTopic(String topic, Class<T> eventType) {
    log.info("Register kafka event listener for topic: {} and event type: {}", topic, eventType);

    stopExistingContainerForTopic(topic);
    var container = createContainer(topic);
    CONTAINERS.put(topic, container);

    container.setupMessageListener((MessageListener<String, String>) record -> {
      log.info("Handling kafka event: {}", record);
      this.addEvent(topic, record);
    });

    container.start();
  }

  private static void stopExistingContainerForTopic(String topic) {
    var topicContainer = CONTAINERS.get(topic);
    if (topicContainer != null) {
      topicContainer.stop();
    }
  }

  public static List<ConsumerRecord<String, String>> getEvents(String topic) {
    return EVENTS.getOrDefault(topic, emptyList());
  }

  public static <T> List<ConsumerRecord<String, T>> getEvents(String topic, Class<T> type) {
    return getEvents(topic, record -> createRecordWithParsedValue(record, parse(record.value(), type)));
  }

  public static <T> List<ConsumerRecord<String, T>> getEvents(String topic, TypeReference<T> typeReference) {
    return getEvents(topic, record -> createRecordWithParsedValue(record, parse(record.value(), typeReference)));
  }

  public static <T> List<ConsumerRecord<String, T>> getEvents(String topic,
    Function<ConsumerRecord<String, String>, ConsumerRecord<String, T>> consumerRecordValueMapper) {
    var foundEvents = EVENTS.getOrDefault(topic, emptyList());
    return foundEvents.stream().map(consumerRecordValueMapper).collect(toList());
  }

  public static void removeAllEvents() {
    EVENTS.clear();
  }

  public static void stopAllContainers() {
    CONTAINERS.forEach((type, container) -> container.stop());
    CONTAINERS.clear();
  }

  private void addEvent(String topic, ConsumerRecord<String, String> record) {
    EVENTS.computeIfAbsent(topic, k -> new ArrayList<>()).add(record);
  }

  private KafkaMessageListenerContainer<String, String> createContainer(String topic) {
    var consumer = new DefaultKafkaConsumerFactory<String, String>(kafkaProperties.buildConsumerProperties(null));
    log.info("Consumer config: {}", kafkaProperties.buildConsumerProperties(null));

    configureDeserializers(consumer);

    var containerProperties = new ContainerProperties(topic);
    var container = new KafkaMessageListenerContainer<>(consumer, containerProperties);
    container.setCommonErrorHandler(new CommonLoggingErrorHandler());
    return container;
  }

  private static void configureDeserializers(DefaultKafkaConsumerFactory<String, String> consumer) {
    consumer.setValueDeserializer(new StringDeserializer());
    consumer.setKeyDeserializer(new StringDeserializer());
  }

  @NotNull
  private static <T> ConsumerRecord<String, T> createRecordWithParsedValue(ConsumerRecord<String, String> record, T v) {
    return new ConsumerRecord<>(record.topic(), record.partition(), record.offset(), record.timestamp(),
      record.timestampType(), record.serializedKeySize(), record.serializedValueSize(), record.key(), v,
      record.headers(), record.leaderEpoch());
  }
}
