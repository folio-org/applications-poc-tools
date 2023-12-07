package org.folio.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.admin.NewTopic;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@UnitTest
class KafkaUtilsTest {

  @BeforeAll
  static void beforeAll() {
    System.setProperty("env", "testenv");
  }

  @AfterAll
  static void afterAll() {
    System.clearProperty("env");
  }

  @Test
  void getEnvTopicName_positive() {
    var result = KafkaUtils.getEnvTopicName("test");
    assertThat(result).isEqualTo("testenv.test");
  }

  @Test
  void getTenantTopicName_positive() {
    var result = KafkaUtils.getTenantTopicName("test-topic", "test-tenant");
    assertThat(result).isEqualTo("testenv.test-tenant.test-topic");
  }

  @Test
  void createTopic_positive() {
    var result = KafkaUtils.createTopic("test-topic", 10, (short) 3);
    assertThat(result).isEqualTo(new NewTopic("test-topic", 10, (short) 3));
  }
}
