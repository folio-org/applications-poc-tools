package org.folio.integration.kafka.consumer.recover;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.integration.kafka.consumer.confirmation.ResourceResultEventPublisher;
import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
@UnitTest
@ExtendWith(MockitoExtension.class)
class ResourceResultEventPublishingRecovererTest {

  private static final String EVENT_ID = "5f26fe20-d7bc-11ef-9cd2-0242ac120002";
  private static final String TENANT_ID = "test";
  private static final String RESOURCE_NAME = "Scheduled Job";
  private static final String MODULE_ID = "mod-foo-1.0.0";
  private static final String TOPIC = "folio.mgr-tenant-entitlements.scheduled-job";

  @Mock private ResourceResultEventPublisher eventPublisher;
  @Mock private ModuleIdExtractor moduleIdExtractor;
  @InjectMocks private ResourceResultEventPublishingRecoverer recoverer;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(eventPublisher, moduleIdExtractor);
  }

  @Test
  void accept_positive_resourceEventValue_publishesFailureEvent() {
    var resourceEvent = resourceEvent();
    var rec = new ConsumerRecord<String, Object>(TOPIC, 0, 0L, TENANT_ID, resourceEvent);
    var exception = new RuntimeException("processing failed");
    when(moduleIdExtractor.apply(resourceEvent)).thenReturn(MODULE_ID);

    recoverer.accept(rec, exception);

    verify(moduleIdExtractor).apply(resourceEvent);
    verify(eventPublisher).publishFailureFor(
      eq(resourceEvent),
      argThat(e -> e instanceof RuntimeException && "processing failed".equals(e.getMessage())),
      eq(MODULE_ID)
    );
  }

  @Test
  void accept_negative_nonResourceEventValue_doesNotPublish() {
    var rec = new ConsumerRecord<String, Object>(TOPIC, 0, 0L, TENANT_ID, "plain-string-value");

    recoverer.accept(rec, new RuntimeException("error"));

    // verifyNoMoreInteractions in tearDown asserts neither publisher nor extractor was called
  }

  @Test
  void accept_positive_extractorReturnsNull_publishesWithNullModuleId() {
    var resourceEvent = resourceEvent();
    var rec = new ConsumerRecord<String, Object>(TOPIC, 0, 0L, TENANT_ID, resourceEvent);
    when(moduleIdExtractor.apply(resourceEvent)).thenReturn(null);

    recoverer.accept(rec, new RuntimeException("error"));

    verify(moduleIdExtractor).apply(resourceEvent);
    verify(eventPublisher).publishFailureFor(eq(resourceEvent), any(Exception.class), eq(null));
  }

  private static ResourceEvent<Object> resourceEvent() {
    return ResourceEvent.<Object>baseBuilder()
      .id(EVENT_ID)
      .tenant(TENANT_ID)
      .resourceName(RESOURCE_NAME)
      .build();
  }
}
