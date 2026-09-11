package org.folio.integration.kafka.consumer.recover;

import static org.assertj.core.api.Assertions.assertThatNoException;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class LoggingRecovererTest {

  private final LoggingRecoverer recoverer = new LoggingRecoverer();

  @Test
  void accept_positive_logsErrorWithoutThrowing() {
    var rec = new ConsumerRecord<>("topic", 0, 0L, "key", "value");
    var exception = new RuntimeException("processing failed");

    assertThatNoException().isThrownBy(() -> recoverer.accept(rec, exception));
  }
}
