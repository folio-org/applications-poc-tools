package org.folio.integration.kafka.consumer.recover;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.integration.kafka.consumer.confirmation.ResourceResultEventPublisher;
import org.folio.integration.kafka.model.ResourceEvent;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

@Log4j2
@Component
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
