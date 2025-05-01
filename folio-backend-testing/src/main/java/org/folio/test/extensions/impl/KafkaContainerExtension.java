package org.folio.test.extensions.impl;

import static org.testcontainers.utility.DockerImageName.parse;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KafkaContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String SPRING_PROPERTY_NAME = "spring.kafka.bootstrap-servers";
  private static final DockerImageName KAFKA_IMAGE = parse("apache/kafka-native:3.8.0");
  private static final KafkaContainer CONTAINER = new KafkaContainer(KAFKA_IMAGE)
    .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "false");

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    System.setProperty(SPRING_PROPERTY_NAME, CONTAINER.getBootstrapServers());
  }

  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty(SPRING_PROPERTY_NAME);
  }
}
