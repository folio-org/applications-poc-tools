package org.folio.integration.kafka.consumer.confirmation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.integration.kafka.model.ResourceResultStatus.FAILURE;
import static org.folio.integration.kafka.model.ResourceResultStatus.SUCCESS;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.integration.kafka.model.ResourceResultEvent;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ResourceResultEventPublisherTest {

  private static final String EVENT_ID = "5f26fe20-d7bc-11ef-9cd2-0242ac120002";
  private static final String TENANT_ID = "test";
  private static final String RESOURCE_NAME = "Scheduled Job";
  private static final String MODULE_ID = "mod-foo-1.0.0";

  @Mock private ApplicationEventPublisher eventPublisher;
  @InjectMocks private ResourceResultEventPublisher publisher;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(eventPublisher);
  }

  @Test
  void publishSuccessFor_positive_publishesSuccessEventWithNullDetails() {
    var resourceEvent = resourceEvent();

    publisher.publishSuccessFor(resourceEvent, MODULE_ID);

    var captor = ArgumentCaptor.forClass(ResourceResultEvent.class);
    verify(eventPublisher).publishEvent((Object) captor.capture());
    var published = captor.getValue();
    assertThat(published.getId()).isEqualTo(EVENT_ID);
    assertThat(published.getTenant()).isEqualTo(TENANT_ID);
    assertThat(published.getResourceName()).isEqualTo(RESOURCE_NAME);
    assertThat(published.getModuleId()).isEqualTo(MODULE_ID);
    assertThat(published.getStatus()).isEqualTo(SUCCESS);
    assertThat(published.getDetails()).isNull();
  }

  @Test
  void publishFailureFor_positive_publishesFailureEventWithExceptionDetails() {
    var resourceEvent = resourceEvent();
    var exception = new RuntimeException("processing failed");

    publisher.publishFailureFor(resourceEvent, exception, MODULE_ID);

    var captor = ArgumentCaptor.forClass(ResourceResultEvent.class);
    verify(eventPublisher).publishEvent((Object) captor.capture());
    var published = captor.getValue();
    assertThat(published.getId()).isEqualTo(EVENT_ID);
    assertThat(published.getTenant()).isEqualTo(TENANT_ID);
    assertThat(published.getResourceName()).isEqualTo(RESOURCE_NAME);
    assertThat(published.getModuleId()).isEqualTo(MODULE_ID);
    assertThat(published.getStatus()).isEqualTo(FAILURE);
    assertThat(published.getDetails()).contains("processing failed");
  }

  private static ResourceEvent<Object> resourceEvent() {
    return ResourceEvent.<Object>baseBuilder()
      .id(EVENT_ID)
      .tenant(TENANT_ID)
      .resourceName(RESOURCE_NAME)
      .build();
  }
}
