package org.folio.integration.kafka.consumer.configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the event-confirmation subsystem,
 * bound to the {@code application.event-confirmation} prefix.
 *
 * <p>All properties take effect only when
 * {@code application.event-confirmation.enabled=true}. See
 * {@link EventConfirmationConfiguration} for the full list of beans that are activated.
 */
@Data
@Validated
@ConfigurationProperties("application.event-confirmation")
public class EventConfirmationProperties {

  /**
   * Flag to enable/disable sending confirmation events.
   */
  private boolean enabled;

  /**
   * Kafka topic to which {@link org.folio.integration.kafka.model.ResourceResultEvent}
   * confirmations are published. Must not be blank when the feature is enabled.
   */
  @NotBlank
  private String topic;

  /**
   * Listener settings for {@code SUCCESS} result events.
   * Defaults to non-transactional mode.
   */
  private ListenerConfig successListener = new ListenerConfig();

  /**
   * Listener settings for {@code FAILURE} result events.
   * Defaults to non-transactional mode.
   */
  private ListenerConfig failureListener = new ListenerConfig();

  /**
   * Per-listener transactional binding options.
   */
  @Data
  public static class ListenerConfig {

    /**
     * When {@code true}, the listener is wrapped in a
     * {@link org.springframework.transaction.event.TransactionalApplicationListenerAdapter}
     * and fires according to {@link #transactionPhase}.
     * When {@code false} (default), the listener fires immediately on event publication.
     */
    private boolean transactional = false;

    /**
     * The transaction phase at which the listener fires when {@link #transactional} is
     * {@code true}. Defaults to {@link TransactionPhase#AFTER_COMMIT}.
     */
    private TransactionPhase transactionPhase = TransactionPhase.AFTER_COMMIT;
  }
}
