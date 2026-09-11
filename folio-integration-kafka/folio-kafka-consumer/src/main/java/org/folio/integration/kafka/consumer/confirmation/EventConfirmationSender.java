package org.folio.integration.kafka.consumer.confirmation;

import org.folio.integration.kafka.model.ResourceResultEvent;

/**
 * Contract for sending a {@link ResourceResultEvent} confirmation after a resource event
 * has been processed.
 *
 * <p>The default implementation {@link KafkaEventConfirmationSender} publishes the event to
 * a Kafka topic. Applications may supply their own bean named {@code eventConfirmationSender}
 * to replace the default implementation entirely.
 *
 * @see KafkaEventConfirmationSender
 * @see org.folio.integration.kafka.consumer.configuration.EventConfirmationConfiguration
 */
public interface EventConfirmationSender {

  /**
   * Invoked after a resource event was processed successfully.
   *
   * @param event the result event carrying {@code SUCCESS} status and the resource details
   */
  void onSuccessfulResourceResult(ResourceResultEvent event);

  /**
   * Invoked after all retry attempts for a resource event have been exhausted.
   *
   * @param event the result event carrying {@code FAILURE} status and the exception details
   */
  void onFailedResourceResult(ResourceResultEvent event);
}
