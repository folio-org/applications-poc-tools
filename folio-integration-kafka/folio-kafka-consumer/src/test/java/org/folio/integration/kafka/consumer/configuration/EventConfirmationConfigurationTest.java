package org.folio.integration.kafka.consumer.configuration;

import static org.folio.integration.kafka.model.ResourceResultStatus.FAILURE;
import static org.folio.integration.kafka.model.ResourceResultStatus.SUCCESS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.folio.integration.kafka.consumer.confirmation.EventConfirmationSender;
import org.folio.integration.kafka.model.ResourceResultEvent;
import org.folio.integration.kafka.model.ResourceResultStatus;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.PayloadApplicationEvent;

@UnitTest
@ExtendWith(MockitoExtension.class)
class EventConfirmationConfigurationTest {

  @Mock private EventConfirmationSender sender;

  private EventConfirmationConfiguration.Enabled configuration;

  @BeforeEach
  void setUp() {
    configuration = new EventConfirmationConfiguration.Enabled();
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(sender);
  }

  @Test
  void successfulResourceResultEventListener_positive_successEvent_delegatesToSender() {
    var listener = configuration.successfulResourceResultEventListener(sender);
    var event = payloadEvent(resourceResultEvent(SUCCESS));

    listener.onApplicationEvent(event);

    verify(sender).onSuccessfulResourceResult(event.getPayload());
  }

  @Test
  void successfulResourceResultEventListener_positive_failureEvent_ignored() {
    var listener = configuration.successfulResourceResultEventListener(sender);

    listener.onApplicationEvent(payloadEvent(resourceResultEvent(FAILURE)));

    // verifyNoMoreInteractions in tearDown asserts no sender call
  }

  @Test
  void failedResourceResultEventListener_positive_failureEvent_delegatesToSender() {
    var listener = configuration.failedResourceResultEventListener(sender);
    var event = payloadEvent(resourceResultEvent(FAILURE));

    listener.onApplicationEvent(event);

    verify(sender).onFailedResourceResult(event.getPayload());
  }

  @Test
  void failedResourceResultEventListener_positive_successEvent_ignored() {
    var listener = configuration.failedResourceResultEventListener(sender);

    listener.onApplicationEvent(payloadEvent(resourceResultEvent(SUCCESS)));

    // verifyNoMoreInteractions in tearDown asserts no sender call
  }

  private static ResourceResultEvent resourceResultEvent(ResourceResultStatus status) {
    return ResourceResultEvent.builder().id("id").tenant("test").status(status).build();
  }

  private static PayloadApplicationEvent<ResourceResultEvent> payloadEvent(ResourceResultEvent payload) {
    return new PayloadApplicationEvent<>(new Object(), payload);
  }
}
