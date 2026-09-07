package org.folio.integration.kafka.consumer.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.consumer.confirmation.EventConfirmationSender;
import org.folio.integration.kafka.consumer.confirmation.KafkaEventConfirmationSender;
import org.folio.integration.kafka.model.ResourceResultEvent;
import org.folio.integration.kafka.model.ResourceResultStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.event.TransactionalApplicationListener;
import org.springframework.transaction.event.TransactionalApplicationListenerAdapter;

/**
 * Spring auto-configuration for the event-confirmation subsystem.
 *
 * <p>This configuration is activated when {@code application.event-confirmation.enabled=true} is
 * set. It registers:
 * <ul>
 *   <li>An {@link AsyncTaskExecutor} bean named {@code asyncEvtTaskExecutor} used by the default
 *       sender to dispatch Kafka sends off the consumer thread.
 *   <li>A {@link EventConfirmationSender} bean named {@code defaultEventConfirmationSender} backed
 *       by {@link KafkaEventConfirmationSender}. Both beans are suppressed when the application
 *       already defines a bean named {@code eventConfirmationSender}.
 *   <li>Four {@link org.springframework.context.ApplicationListener} beans that route
 *       {@link org.springframework.context.PayloadApplicationEvent} payloads to the sender —
 *       two for {@code SUCCESS} and two for {@code FAILURE}, with non-transactional and
 *       transactional variants selectable via
 *       {@code application.event-confirmation.success-listener.transactional} and
 *       {@code application.event-confirmation.failure-listener.transactional}.
 * </ul>
 *
 * @see EventConfirmationProperties
 * @see EventConfirmationSender
 */
@Log4j2
@Configuration
@ConditionalOnBooleanProperty(prefix = "application.event-confirmation", name = "enabled")
@Import({EventConfirmationProperties.class})
@RequiredArgsConstructor
public class EventConfirmationConfiguration {

  private final EventConfirmationProperties eventConfirmationProperties;

  @Bean(name = "asyncEvtTaskExecutor")
  @ConditionalOnMissingBean(name = "eventConfirmationSender")
  public AsyncTaskExecutor asyncEvtTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(3);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("AsyncEvt-");
    executor.initialize();
    return executor;
  }

  @Bean("defaultEventConfirmationSender")
  @ConditionalOnMissingBean(name = "eventConfirmationSender")
  public EventConfirmationSender eventConfirmationSender(KafkaTemplate<String, ResourceResultEvent> kafkaTemplate,
    @Qualifier("asyncEvtTaskExecutor") AsyncTaskExecutor asyncExecutor) {
    log.info("Event confirmation is enabled.");

    return new KafkaEventConfirmationSender(eventConfirmationProperties.getTopic(), kafkaTemplate, asyncExecutor);
  }

  @Bean
  @ConditionalOnBooleanProperty(prefix = "application.event-confirmation.success-listener", name = "transactional",
    havingValue = false, matchIfMissing = true)
  public ApplicationListener<PayloadApplicationEvent<ResourceResultEvent>> successfulResourceResultEventListener(
    EventConfirmationSender eventConfirmationSender) {
    log.debug("Registering non-transactional successful resource result event listener.");

    return baseSuccessListener(eventConfirmationSender);
  }

  @Bean
  @ConditionalOnBooleanProperty(prefix = "application.event-confirmation.success-listener", name = "transactional")
  @SuppressWarnings("checkstyle:LineLength")
  public TransactionalApplicationListener<PayloadApplicationEvent<ResourceResultEvent>> successfulResourceResultEventListenerTrx(
    EventConfirmationSender eventConfirmationSender) {
    log.debug("Registering transactional successful resource result event listener.");

    var adapter = new TransactionalApplicationListenerAdapter<>(baseSuccessListener(eventConfirmationSender));
    adapter.setTransactionPhase(eventConfirmationProperties.getSuccessListener().getTransactionPhase());
    return adapter;
  }

  @Bean
  @ConditionalOnBooleanProperty(prefix = "application.event-confirmation.failure-listener", name = "transactional",
    havingValue = false, matchIfMissing = true)
  public ApplicationListener<PayloadApplicationEvent<ResourceResultEvent>> failedResourceResultEventListener(
    EventConfirmationSender eventConfirmationSender) {
    log.debug("Registering non-transactional failed resource result event listener.");

    return baseFailureListener(eventConfirmationSender);
  }

  @Bean
  @ConditionalOnBooleanProperty(prefix = "application.event-confirmation.failure-listener", name = "transactional")
  @SuppressWarnings("checkstyle:LineLength")
  public TransactionalApplicationListener<PayloadApplicationEvent<ResourceResultEvent>> failedResourceResultEventListenerTrx(
    EventConfirmationSender eventConfirmationSender) {
    log.debug("Registering transactional failed resource result event listener.");

    var adapter = new TransactionalApplicationListenerAdapter<>(baseFailureListener(eventConfirmationSender));
    adapter.setTransactionPhase(eventConfirmationProperties.getFailureListener().getTransactionPhase());
    return adapter;
  }

  private static ApplicationListener<PayloadApplicationEvent<ResourceResultEvent>> baseSuccessListener(
    EventConfirmationSender eventConfirmationSender) {
    return event -> {
      var resourceResultEvent = event.getPayload();
      if (resourceResultEvent.getStatus() == ResourceResultStatus.SUCCESS) {
        eventConfirmationSender.onSuccessfulResourceResult(resourceResultEvent);
      }
    };
  }

  private static ApplicationListener<PayloadApplicationEvent<ResourceResultEvent>> baseFailureListener(
    EventConfirmationSender eventConfirmationSender) {
    return event -> {
      var resourceResultEvent = event.getPayload();
      if (resourceResultEvent.getStatus() == ResourceResultStatus.FAILURE) {
        eventConfirmationSender.onFailedResourceResult(resourceResultEvent);
      }
    };
  }
}
