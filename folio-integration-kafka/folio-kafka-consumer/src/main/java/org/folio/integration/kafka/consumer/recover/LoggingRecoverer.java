package org.folio.integration.kafka.consumer.recover;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class LoggingRecoverer implements ConsumerRecordRecoverer {

  @Override
  public void accept(ConsumerRecord<?, ?> consumerRecord, Exception exception) {
    log.error("Failed to process record: {}", consumerRecord, exception);
  }
}
