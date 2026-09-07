package org.folio.integration.kafka.consumer.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties("application.event-confirmation")
public class EventConfirmationProperties {

  /**
   * Flag to enable/disable sending confirmation events.
   */
  private boolean enabled;

  /**
   * Kafka topic for sending confirmation events.
   */
  @NotBlank
  private String topic;

  private ListenerConfig successListener = new ListenerConfig();
  private ListenerConfig failureListener = new ListenerConfig();

  @Data
  public static class ListenerConfig {

    private boolean transactional = false;

    private TransactionPhase transactionPhase = TransactionPhase.AFTER_COMMIT;
  }
}
