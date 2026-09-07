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

@Log4j2
@Component
@RequiredArgsConstructor
public class ResourceResultEventPublisher {

  private final ApplicationEventPublisher eventPublisher;

  public void publishSuccessFor(ResourceEvent<?> resourceEvent, String moduleId) {
    publish(resourceEvent, null, moduleId);
  }

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
