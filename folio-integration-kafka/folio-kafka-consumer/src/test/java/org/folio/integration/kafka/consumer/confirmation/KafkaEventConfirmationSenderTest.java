package org.folio.integration.kafka.consumer.confirmation;

import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.folio.integration.kafka.consumer.KafkaTenantHeaders.FOLIO_TENANT_ID;
import static org.folio.integration.kafka.model.ResourceResultStatus.FAILURE;
import static org.folio.integration.kafka.model.ResourceResultStatus.SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.folio.integration.kafka.model.ResourceResultEvent;
import org.folio.integration.kafka.model.ResourceResultStatus;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.kafka.core.KafkaTemplate;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KafkaEventConfirmationSenderTest {

  private static final String TOPIC = "folio.mgr-tenant-entitlements.resource-result";
  private static final String EVENT_ID = "5f26fe20-d7bc-11ef-9cd2-0242ac120002";
  private static final String TENANT_ID = "test";
  private static final String MODULE_ID = "mod-foo-1.0.0";

  @Mock private KafkaTemplate<String, ResourceResultEvent> kafkaTemplate;
  @Mock private AsyncTaskExecutor asyncExecutor;

  private KafkaEventConfirmationSender sender;

  @BeforeEach
  void setUp() {
    doAnswer(inv -> {
      ((Runnable) inv.getArgument(0)).run();
      return null;
    })
      .when(asyncExecutor).execute(any(Runnable.class));
    sender = new KafkaEventConfirmationSender(TOPIC, kafkaTemplate, asyncExecutor);
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(kafkaTemplate, asyncExecutor);
  }

  @Test
  void onSuccessfulResourceResult_positive_sendsToCorrectTopic() {
    var event = resourceResultEvent(SUCCESS);
    when(kafkaTemplate.send(argThat(isRecordFor(event)))).thenReturn(CompletableFuture.completedFuture(null));

    sender.onSuccessfulResourceResult(event);

    verify(asyncExecutor).execute(any(Runnable.class));
    verify(kafkaTemplate).send(argThat(isRecordFor(event)));
  }

  @Test
  void onSuccessfulResourceResult_negative_sendFails_noExceptionPropagated() {
    var event = resourceResultEvent(SUCCESS);
    when(kafkaTemplate.send(argThat(isRecordFor(event))))
      .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker unavailable")));

    sender.onSuccessfulResourceResult(event);

    verify(asyncExecutor).execute(any(Runnable.class));
    verify(kafkaTemplate).send(argThat(isRecordFor(event)));
  }

  @Test
  void onFailedResourceResult_positive_sendsToCorrectTopic() {
    var event = resourceResultEvent(FAILURE);
    when(kafkaTemplate.send(argThat(isRecordFor(event)))).thenReturn(CompletableFuture.completedFuture(null));

    sender.onFailedResourceResult(event);

    verify(asyncExecutor).execute(any(Runnable.class));
    verify(kafkaTemplate).send(argThat(isRecordFor(event)));
  }

  private static ArgumentMatcherForRecord isRecordFor(ResourceResultEvent event) {
    return new ArgumentMatcherForRecord(event);
  }

  private static ResourceResultEvent resourceResultEvent(ResourceResultStatus status) {
    return ResourceResultEvent.builder()
      .id(EVENT_ID).tenant(TENANT_ID).moduleId(MODULE_ID).status(status).build();
  }

  private record ArgumentMatcherForRecord(ResourceResultEvent event)
    implements ArgumentMatcher<ProducerRecord<String, ResourceResultEvent>> {

    @Override
    public boolean matches(ProducerRecord<String, ResourceResultEvent> event) {
      var tenantBytes = TENANT_ID.getBytes(StandardCharsets.UTF_8);
      return event != null
        && TOPIC.equals(event.topic())
        && TENANT_ID.equals(event.key())
        && this.event.equals(event.value())
        && event.headers().lastHeader(TENANT) != null
        && Arrays.equals(event.headers().lastHeader(TENANT).value(), tenantBytes)
        && event.headers().lastHeader(FOLIO_TENANT_ID) != null
        && Arrays.equals(event.headers().lastHeader(FOLIO_TENANT_ID).value(), tenantBytes);
    }
  }
}
