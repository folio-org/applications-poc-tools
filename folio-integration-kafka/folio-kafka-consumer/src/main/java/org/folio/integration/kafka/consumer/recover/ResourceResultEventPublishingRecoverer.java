package org.folio.integration.kafka.consumer.recover;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.integration.kafka.consumer.confirmation.ResourceResultEventPublisher;
import org.folio.integration.kafka.model.ResourceEvent;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;

/**
 * {@link ConsumerRecordRecoverer} that publishes a {@code FAILURE} confirmation event when
 * all retry attempts for a Kafka consumer record have been exhausted.
 *
 * <p>If the consumer record's value is a {@link ResourceEvent}, this recoverer delegates to
 * {@link ResourceResultEventPublisher#publishFailureFor} so the originating caller can learn
 * that processing failed. Records whose value is not a {@link ResourceEvent} are logged at
 * {@code ERROR} level and silently dropped.
 *
 * <p>Wire this bean as the dead-letter recoverer in the retry configuration alongside a
 * {@link ModuleIdExtractor} bean that provides the correct module identifier for the
 * consumer module.
 */
@Log4j2
@RequiredArgsConstructor
public class ResourceResultEventPublishingRecoverer implements ConsumerRecordRecoverer {

  private final ResourceResultEventPublisher eventPublisher;
  private final ModuleIdExtractor moduleIdExtractor;

  @Override
  public void accept(ConsumerRecord<?, ?> consumerRecord, Exception exception) {
    if (consumerRecord.value() instanceof ResourceEvent<?> resourceEvent) {
      eventPublisher.publishFailureFor(resourceEvent, exception, moduleIdExtractor.apply(resourceEvent));
    } else {
      log.error("Expected ResourceEvent but got: record = {}. Original exception = {}", consumerRecord, exception);
    }
  }
}
