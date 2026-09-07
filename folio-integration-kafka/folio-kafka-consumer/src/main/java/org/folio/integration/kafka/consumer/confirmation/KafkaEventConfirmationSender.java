package org.folio.integration.kafka.consumer.confirmation;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.model.ResourceResultEvent;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.kafka.core.KafkaTemplate;

@Log4j2
@RequiredArgsConstructor
public class KafkaEventConfirmationSender implements EventConfirmationSender {

  private final String confirmationTopic;
  private final KafkaTemplate<String, ResourceResultEvent> kafkaTemplate;
  private final AsyncTaskExecutor asyncExecutor;

  @Override
  public void onSuccessfulResourceResult(ResourceResultEvent event) {
    asyncExecutor.execute(() -> send(event));
  }

  @Override
  public void onFailedResourceResult(ResourceResultEvent event) {
    asyncExecutor.execute(() -> send(event));
  }

  private void send(ResourceResultEvent resultEvent) {
    var key = resultEvent.getTenant();

    kafkaTemplate.send(confirmationTopic, key, resultEvent).whenComplete((result, exception) -> {
      if (exception != null) {
        // A lost confirmation leaves the originating entitlement stage IN_PROGRESS until the sender's
        // async-confirmation timeout expires, so the failure must be visible in the log.
        log.error("Failed to send resource result confirmation event: eventId = {}, tenant ={}, moduleId = {}, "
          + "status = {}",
          resultEvent.getId(), resultEvent.getTenant(), resultEvent.getModuleId(), resultEvent.getStatus(), exception);
        return;
      }

      log.info("Resource result confirmation event sent successfully: eventId = {}, tenant ={}, moduleId = {}, "
          + "status = {}",
        resultEvent.getId(), resultEvent.getTenant(), resultEvent.getModuleId(), resultEvent.getStatus());
    });
  }
}
