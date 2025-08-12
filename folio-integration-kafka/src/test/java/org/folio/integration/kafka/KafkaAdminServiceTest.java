package org.folio.integration.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.integration.kafka.KafkaUtils.getTenantTopicName;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.folio.common.configuration.properties.FolioEnvironment;
import org.folio.integration.kafka.KafkaAdminServiceTest.TestContextConfiguration;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@UnitTest
@Import(TestContextConfiguration.class)
@SpringBootTest(classes = KafkaAdminService.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaAdminServiceTest {

  @Autowired private KafkaAdminService kafkaAdminService;
  @Autowired private ApplicationContext applicationContext;
  @MockBean private KafkaAdmin kafkaAdmin;

  @Test
  void createKafkaTopics_positive_newTopic() {
    var topic = KafkaUtils.createTopic(getTenantTopicName("topic1", "test"), 10, null);

    kafkaAdminService.createTopic(topic);

    verify(kafkaAdmin).initialize();
    var beansOfType = applicationContext.getBeansOfType(NewTopic.class);
    assertThat(beansOfType.values()).containsExactlyInAnyOrderElementsOf(List.of(
      new NewTopic("folio.test.topic1", Optional.of(10), Optional.empty()),
      new NewTopic("folio.test.topic3", Optional.of(30), Optional.of((short) -1))
    ));
  }

  @Test
  void createKafkaTopics_positive_existingTopic() {
    var topic = KafkaUtils.createTopic(getTenantTopicName("topic3", "test"), 10, null);
    kafkaAdminService.createTopic(topic);
    verify(kafkaAdmin).initialize();

    var beansOfType = applicationContext.getBeansOfType(NewTopic.class);
    assertThat(beansOfType.values()).containsExactlyInAnyOrderElementsOf(List.of(
      new NewTopic("folio.test.topic3", Optional.of(30), Optional.of((short) -1))
    ));
  }

  @Test
  void deleteKafkaTopics_positive() {
    var topicName = "folio.test_tenant.test_topic";
    var future = KafkaFuture.completedFuture(Set.of(topicName));
    var listTopicResult = mock(ListTopicsResult.class);
    when(listTopicResult.names()).thenReturn(future);

    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      var deleteTopicsResult = mock(DeleteTopicsResult.class);
      when(kafkaClient.listTopics()).thenReturn(listTopicResult);
      when(kafkaClient.deleteTopics(anyCollection())).thenReturn(deleteTopicsResult);
      when(deleteTopicsResult.all()).thenReturn(KafkaFuture.completedFuture(mock(Void.class)));
      kafkaAdminService.deleteTopics(List.of(topicName));
    }

    verify(kafkaClient).listTopics();
    verify(kafkaClient).deleteTopics(List.of(topicName));
  }

  @Test
  void deleteKafkaTopics_positive_withNoMatchingTopic() {
    var future = KafkaFuture.completedFuture(Set.of("folio.test_tenant.test_topic2"));
    var listTopicResult = mock(ListTopicsResult.class);
    when(listTopicResult.names()).thenReturn(future);

    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      when(kafkaClient.listTopics()).thenReturn(listTopicResult);
      var topicName = "folio.test_tenant.test_topic";
      kafkaAdminService.deleteTopics(List.of(topicName));
    }

    verify(kafkaClient).listTopics();
    verify(kafkaClient, never()).deleteTopics(List.of("folio.test_tenant.test_topic"));
  }

  @Test
  void deleteKafkaTopics_positive_listTopicsIsNull() {
    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      when(kafkaClient.listTopics()).thenReturn(null);
      var topicName = "test_tenant";
      kafkaAdminService.deleteTopics(List.of(topicName));
    }

    verify(kafkaClient).listTopics();
  }

  @Test
  void deleteKafkaTopics_positive_listTopicNamesIsNull() {
    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      var listTopicsResult = mock(ListTopicsResult.class);
      when(kafkaClient.listTopics()).thenReturn(listTopicsResult);
      when(listTopicsResult.names()).thenReturn(null);
      var topicName = "test_tenant";
      kafkaAdminService.deleteTopics(List.of(topicName));
    }

    verify(kafkaClient).listTopics();
  }

  @Test
  void deleteKafkaTopics_positive_withNoTopicsFound() {
    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      when(kafkaClient.listTopics()).thenReturn(null);
      var topicName = "test_tenant";
      kafkaAdminService.deleteTopics(List.of(topicName));
    }

    verify(kafkaClient).listTopics();
  }

  @Test
  void deleteKafkaTopics_negative_shouldHandleExceptionWithNoDeleteResult() {
    var topicName = "test.topic";
    var future = KafkaFuture.completedFuture(Set.of(topicName));
    var listTopicResult = mock(ListTopicsResult.class);
    when(listTopicResult.names()).thenReturn(future);

    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      var deleteTopicsResult = mock(DeleteTopicsResult.class);
      when(kafkaClient.listTopics()).thenReturn(listTopicResult);
      when(kafkaClient.deleteTopics(anyCollection())).thenReturn(deleteTopicsResult);
      when(deleteTopicsResult.all()).thenThrow(new KafkaException("There was an error while deleting topics"));

      var topicsToDelete = List.of(topicName);
      assertThatThrownBy(() -> kafkaAdminService.deleteTopics(topicsToDelete))
        .isInstanceOf(KafkaException.class)
        .hasMessage("Failed to delete topics: [test.topic]");
    }

    verify(kafkaClient).listTopics();
  }

  @Test
  void findTopics_positive_someExist() {
    var requestedTopics = List.of("topicA", "topicB", "topicC");
    var existingTopics = Set.of("topicA", "topicC", "topicD");
    var future = KafkaFuture.completedFuture(existingTopics);
    var listTopicsResult = mock(ListTopicsResult.class);
    when(listTopicsResult.names()).thenReturn(future);

    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      when(kafkaClient.listTopics()).thenReturn(listTopicsResult);
      var found = kafkaAdminService.findTopics(requestedTopics);
      assertThat(found).containsExactlyInAnyOrder("topicA", "topicC");
    }
    verify(kafkaClient).listTopics();
  }

  @Test
  void findTopics_positive_allExist() {
    var requestedTopics = List.of("topicA", "topicB");
    var existingTopics = Set.of("topicA", "topicB");
    var future = KafkaFuture.completedFuture(existingTopics);
    var listTopicsResult = mock(ListTopicsResult.class);
    when(listTopicsResult.names()).thenReturn(future);

    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      when(kafkaClient.listTopics()).thenReturn(listTopicsResult);
      var found = kafkaAdminService.findTopics(requestedTopics);
      assertThat(found).containsExactlyInAnyOrderElementsOf(requestedTopics);
    }
    verify(kafkaClient).listTopics();
  }

  @Test
  void findTopics_positive_noneExist() {
    var requestedTopics = List.of("topicA", "topicB");
    var existingTopics = Set.of("topicC", "topicD");
    var future = KafkaFuture.completedFuture(existingTopics);
    var listTopicsResult = mock(ListTopicsResult.class);
    when(listTopicsResult.names()).thenReturn(future);

    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      when(kafkaClient.listTopics()).thenReturn(listTopicsResult);
      var found = kafkaAdminService.findTopics(requestedTopics);
      assertThat(found).isEmpty();
    }
    verify(kafkaClient).listTopics();
  }

  @ParameterizedTest
  @NullAndEmptySource
  void findTopics_positive_emptyInput(List<String> topics) {
    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      var found = kafkaAdminService.findTopics(topics);
      assertThat(found).isEmpty();
    }
  }

  @Test
  void findTopics_positive_listTopicsIsNull() {
    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      when(kafkaClient.listTopics()).thenReturn(null);
      var found = kafkaAdminService.findTopics(List.of("topicA"));
      assertThat(found).isEmpty();
    }
    verify(kafkaClient).listTopics();
  }

  @Test
  void findTopics_positive_listTopicNamesIsNull() {
    var listTopicsResult = mock(ListTopicsResult.class);
    when(listTopicsResult.names()).thenReturn(null);
    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      when(kafkaClient.listTopics()).thenReturn(listTopicsResult);
      var found = kafkaAdminService.findTopics(List.of("topicA"));
      assertThat(found).isEmpty();
    }
    verify(kafkaClient).listTopics();
  }

  @Test
  void findTopics_negative_shouldHandleException() {
    var listTopicsResult = mock(ListTopicsResult.class);
    var kf = new KafkaFutureImpl<Set<String>>();
    kf.completeExceptionally(new RuntimeException("Kafka error"));

    when(listTopicsResult.names()).thenReturn(KafkaFuture.completedFuture(Set.of("topicA")));
    var kafkaClient = mock(AdminClient.class);
    try (var ignored = mockStatic(AdminClient.class, invocation -> kafkaClient)) {
      when(kafkaClient.listTopics()).thenReturn(listTopicsResult);
      when(listTopicsResult.names()).thenReturn(kf);

      var topicsToFind = List.of("topicA");
      assertThatThrownBy(() -> kafkaAdminService.findTopics(topicsToFind))
        .isInstanceOf(KafkaException.class)
        .hasMessageContaining("Failed to find topics by name");
    }
    verify(kafkaClient).listTopics();
  }

  @TestConfiguration
  static class TestContextConfiguration {

    @Bean(name = "folio.test.topic3.topic")
    NewTopic firstTopic() {
      return new NewTopic("folio.test.topic3", 30, (short) -1);
    }

    @Bean
    FolioEnvironment folioEnvironment() {
      var config = new FolioEnvironment();
      config.setEnvironment("folio");
      return config;
    }
  }
}
