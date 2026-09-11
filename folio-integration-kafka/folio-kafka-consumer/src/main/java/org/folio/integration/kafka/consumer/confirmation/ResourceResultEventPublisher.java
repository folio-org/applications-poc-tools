package org.folio.integration.kafka.consumer.confirmation;

import static org.folio.integration.kafka.model.ResourceResultStatus.FAILURE;
import static org.folio.integration.kafka.model.ResourceResultStatus.SUCCESS;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.integration.kafka.model.ResourceResultEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring component that converts a processed {@link ResourceEvent} outcome into a
 * {@link ResourceResultEvent} Spring application event.
 *
 * <p>The published event is picked up by the listeners registered by
 * {@link org.folio.integration.kafka.consumer.configuration.EventConfirmationConfiguration},
 * which forward it to the configured {@link EventConfirmationSender}. Callers are decoupled
 * from the sender implementation and only interact with this publisher.
 *
 * <p>For a success result the {@code details} field is {@code null}. For a failure result it
 * is populated with {@link org.apache.commons.lang3.exception.ExceptionUtils#getMessage}.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class ResourceResultEventPublisher {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * Publishes a {@code SUCCESS} {@link ResourceResultEvent} for the given resource event.
   *
   * @param resourceEvent the originating Kafka event
   * @param moduleId      the module identifier to include in the confirmation; may be {@code null}
   */
  public void publishSuccessFor(ResourceEvent<?> resourceEvent, String moduleId) {
    publish(resourceEvent, null, moduleId);
  }

  /**
   * Publishes a {@code FAILURE} {@link ResourceResultEvent} for the given resource event.
   *
   * @param resourceEvent the originating Kafka event
   * @param exception     the exception that caused processing to fail
   * @param moduleId      the module identifier to include in the confirmation; may be {@code null}
   */
  public void publishFailureFor(ResourceEvent<?> resourceEvent, Exception exception, String moduleId) {
    publish(resourceEvent, exception, moduleId);
  }

  private void publish(ResourceEvent<?> resourceEvent, Exception exception, String moduleId) {
    var resultEvent =  ResourceResultEvent.builder()
      .id(resourceEvent.getId())
      .tenant(resourceEvent.getTenant())
      .resourceName(resourceEvent.getResourceName())
      .moduleId(moduleId)
      .status(exception == null ? SUCCESS : FAILURE)
      .details(exception == null ? null : ExceptionUtils.getMessage(exception))
      .build();

    eventPublisher.publishEvent(resultEvent);
    log.debug("Published resource result event: {}", () -> resultEvent);
  }
}
