package org.folio.integration.kafka;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaAdminService {

  private final KafkaAdmin kafkaAdmin;
  private final ConfigurableBeanFactory beanFactory;

  /**
   * Registers in spring context {@link NewTopic} bean.
   *
   * <p>It will create kafka topic if not exists.</p>
   *
   * @param newTopic - bean to register
   */
  public void createTopic(NewTopic newTopic) {
    var beanName = newTopic.name() + ".topic";
    if (!beanFactory.containsBean(beanName)) {
      log.info("Creating topic: {}", newTopic);
      beanFactory.registerSingleton(beanName, newTopic);
    }

    kafkaAdmin.initialize();
  }

  /**
   * Deletes kafka topics by names.
   *
   * @param topics - {@link List} with {@link String} topic names
   */
  public void deleteTopics(Collection<String> topics) {
    log.info("Deleting topics: {}", topics);
    try (var kafkaClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
      deleteTopicsByName(kafkaClient, topics);
    }
  }

  /**
   * Finds existing kafka topics by names.
   *
   * @param topics - {@link Collection} with {@link String} topic names to find
   * @return {@link Collection} with existing topic names
   */
  public Collection<String> findTopics(Collection<String> topics) {
    try (var kafkaClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
      return findTopicsByName(kafkaClient, topics);
    }
  }

  private static CompletableFuture<Set<String>> listAllTopicNames(AdminClient kafkaClient) {
    var listTopicsResult = kafkaClient.listTopics();
    if (listTopicsResult == null || listTopicsResult.names() == null) {
      log.info("No existing topics found");
      return completedFuture(Set.of());
    }

    return listTopicsResult.names()
      .toCompletionStage()
      .toCompletableFuture();
  }

  private Collection<String> findTopicsByName(AdminClient kafkaClient, Collection<String> topicsToFind) {
    if (CollectionUtils.isEmpty(topicsToFind)) {
      return Set.of();
    }

    var listFuture = listAllTopicNames(kafkaClient)
      .thenApply(existingTopicNames -> SetUtils.intersection(existingTopicNames, Set.copyOf(topicsToFind)))
      .thenApply(SetUtils.SetView::toSet);

    try {
      return listFuture.join();
    } catch (CompletionException | CancellationException exception) {
      throw new KafkaException(String.format("Failed to find topics by name: %s", topicsToFind),
        exception.getCause());
    }
  }

  private static void deleteTopicsByName(AdminClient kafkaClient, Collection<String> topics) {
    var kafkaDeletionFuture = listAllTopicNames(kafkaClient)
      .thenApply(existingTopicNames -> getTopicsToDelete(topics, existingTopicNames))
      .thenApply(topicsToDelete -> deleteTopicsFromKafka(kafkaClient, topicsToDelete))
      .thenApply(KafkaAdminService::finalizeDeletion);

    try {
      kafkaDeletionFuture.join();
    } catch (CompletionException | CancellationException exception) {
      throw new KafkaException(String.format("Failed to delete topics: %s", topics), exception.getCause());
    }
  }

  private static Pair<List<String>, DeleteTopicsResult> deleteTopicsFromKafka(AdminClient client, List<String> topics) {
    if (CollectionUtils.isEmpty(topics)) {
      log.warn("No existing topics to delete");
      return Pair.of(topics, null);
    }

    return Pair.of(topics, client.deleteTopics(topics));
  }

  private static List<String> getTopicsToDelete(Collection<String> topics, Set<String> existingTopics) {
    return topics.stream()
      .filter(existingTopics::contains)
      .collect(toList());
  }

  private static CompletableFuture<Void> finalizeDeletion(Pair<List<String>, DeleteTopicsResult> deleteTopicsRsPair) {
    var deleteTopicsResult = deleteTopicsRsPair.getRight();
    if (deleteTopicsResult == null) {
      return completedFuture(null);
    }

    var topicNames = deleteTopicsRsPair.getLeft();
    return deleteTopicsResult.all()
      .toCompletionStage()
      .toCompletableFuture()
      .thenAccept(result -> log.info("Topics were deleted successfully: {}", topicNames));
  }
}
