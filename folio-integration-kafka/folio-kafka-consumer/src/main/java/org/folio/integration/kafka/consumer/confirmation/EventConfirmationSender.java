package org.folio.integration.kafka.consumer.confirmation;

import org.folio.integration.kafka.model.ResourceResultEvent;

public interface EventConfirmationSender {

  void onSuccessfulResourceResult(ResourceResultEvent event);

  void onFailedResourceResult(ResourceResultEvent event);
}
