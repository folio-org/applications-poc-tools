package org.folio.integration.kafka.consumer.recover;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

/**
 * Simple {@link ConsumerRecordRecoverer} that logs the failed record at {@code ERROR} level.
 *
 * <p>Use this recoverer when event-confirmation feedback is not required — for example, in
 * fire-and-forget consumers where the caller does not expect a result event. For consumers
 * that do require feedback, use {@link ResourceResultEventPublishingRecoverer} instead.
 */
@Log4j2
@Component
public class LoggingRecoverer implements ConsumerRecordRecoverer {

  @Override
  public void accept(ConsumerRecord<?, ?> consumerRecord, Exception exception) {
    log.error("Failed to process record: {}", consumerRecord, exception);
  }
}
