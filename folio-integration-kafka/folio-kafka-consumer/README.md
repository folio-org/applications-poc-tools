# folio-kafka-consumer

Spring Boot consumer library for FOLIO applications. Provides per-listener topic and group-ID
configuration, marker-table filtering support, and event confirmation support for async
entitlement feedback.

## Table of Contents

- [Activation](#activation)
- [Listener Configuration](#listener-configuration)
- [Marker-Table Tenant Filtering](#marker-table-tenant-filtering)
- [Event Confirmation](#event-confirmation)
- [Full Configuration Reference](#full-configuration-reference)

---

## Activation

Add `@EnableKafkaConsumer` to a Spring `@Configuration` class:

```java
@Configuration
@EnableKafkaConsumer
public class KafkaConfiguration { }
```

This annotation imports Kafka consumer properties and event-confirmation support:

| Imported configuration                 | Registers                                                                                                      |
|:---------------------------------------|:---------------------------------------------------------------------------------------------------------------|
| `KafkaConsumerPropertiesConfiguration` | `kafkaConsumerProperties` bean bound to `application.kafka.consumer.*` with a stable, SpEL-friendly bean name |

`@EnableKafkaConsumer` also imports `EventConfirmationConfiguration`, which activates only when
`application.event-confirmation.enabled=true`, plus the always-available confirmation components
`ResourceResultEventPublisher`, `LoggingRecoverer` and `ResourceResultEventPublishingRecoverer`.
`EventConfirmationProperties` is bound and validated only when the subsystem is enabled, and
`ResourceResultEventPublishingRecoverer` resolves the `ModuleIdExtractor` bean lazily, so consumers
that do not use event confirmation need no extra configuration. No component scan of the library
package is required (and none is performed).

---

## Listener Configuration

Per-listener topic patterns and consumer group IDs are bound under
`application.kafka.consumer.listener.<name>`:

```yaml
application:
  kafka:
    consumer:
      listener:
        items:
          topic-pattern: trillium\..*\.inventory\.items   # regex
          group-id: mod-my-service-items-group
          concurrency: 3                                  # default: 1
        orders:
          topic-pattern: trillium\..*\.orders
          group-id: mod-my-service-orders-group
```

Reference these in `@KafkaListener` via SpEL:

```java
@KafkaListener(
    topicPattern = "#{kafkaConsumerProperties.listener['items'].topicPattern}",
    groupId      = "#{kafkaConsumerProperties.listener['items'].groupId}"
)
public void processItem(ResourceEvent<Item> event) { ... }
```

The bean name `kafkaConsumerProperties` is guaranteed regardless of the consuming application's
component-scan path — this is the reason `@EnableKafkaConsumer` uses
`KafkaConsumerPropertiesConfiguration` instead of `@EnableConfigurationProperties`.

---

## Marker-Table Tenant Filtering

Eureka platform modules that consume entitlement-time Kafka events can use
`MarkerTableBasedTenantAwareMessageFilter` to ensure that each module version processes events only
for tenants initialized by that version. This avoids relying on asynchronously refreshed entitlement
state while a tenant is being upgraded.

### Marker table

Each tenant schema must contain a single-row `tenant_module_version` table with these columns:

| Column         | Purpose                                                           |
|:---------------|:------------------------------------------------------------------|
| `id`           | Fixed row identifier; the only supported value is `1`             |
| `module_id`    | Full identifier of the module version that initialized the tenant |
| `updated_date` | Time at which the marker was last updated                         |

Create this table through the module's normal database migration mechanism.

### Configuration

Expose the common deployment flag in the module's `application.yml`:

```yaml
folio:
  kafka:
    tenant-filter:
      enabled: ${KAFKA_TENANT_FILTER_ENABLED:false}
```

Import `TenantModuleVersionConfiguration`. It registers `TenantModuleVersionRepository`,
`TenantModuleVersionService`, and the `markerTableBasedTenantAwareMessageFilter` bean.

The module must provide a `TenantInitializationDetector` that checks a stable, module-specific table
that exists after the tenant schema has been initialized:

```java
@Configuration
@Import(TenantModuleVersionConfiguration.class)
class KafkaConfiguration {

    @Bean
    TenantInitializationDetector tenantInitializationDetector(
        TenantModuleVersionRepository repository) {
        return () -> repository.tableExists("module_data_table");
    }
}
```

Record the current module version after tenant initialization succeeds and clear the marker during
tenant deletion:

```java
tenantModuleVersionService.markCurrentVersion();
tenantModuleVersionService.clear();
```

Apply the filter explicitly to each affected listener:

```java
@KafkaListener(
    topicPattern = "...",
    groupId = "...",
    filter = "markerTableBasedTenantAwareMessageFilter")
public void handleEvent(ConsumerRecord<String, Object> event) {
    // Process the event.
}
```

The filter reads the tenant from `x-okapi-tenant`, falling back to `folio.tenantId`. It filters a
record when the tenant schema records a different module version or predates the marker table.
It accepts records for the current version, uninitialized tenants, empty marker tables, and records
without a tenant header. Filtering is disabled by default.

---

## Event Confirmation

The event confirmation subsystem lets a consumer module send a `ResourceResultEvent` back to the
caller (typically `mgr-tenant-entitlements`) once a `ResourceEvent` has been processed — either
successfully or after all retries are exhausted.

### Enabling event confirmation

Set the following properties in `application.yml` (or equivalent):

```yaml
application:
  event-confirmation:
    enabled: true
    topic: folio.<env>.mgr-tenant-entitlements.resource-result
```

### Publishing a success result

Inject `ResourceResultEventPublisher` into the listener and call `publishSuccessFor` after
successful processing:

```java
@Component
@RequiredArgsConstructor
public class MyEventListener {

    private final ResourceResultEventPublisher confirmationPublisher;
    private final ModuleIdExtractor moduleIdExtractor;

    @KafkaListener(...)
    public void handle(ResourceEvent<MyPayload> event) {
        // ... process event ...
        confirmationPublisher.publishSuccessFor(event, moduleIdExtractor.apply(event));
    }
}
```

### Wiring the failure recoverer

To publish a failure confirmation when all retries are exhausted, configure
`ResourceResultEventPublishingRecoverer` as the dead-letter recoverer in the retry back-off policy:

```java
@Bean
public CommonErrorHandler kafkaErrorHandler(ResourceResultEventPublishingRecoverer recoverer) {
    var backOff = new FixedBackOff(1000L, 3L);
    return new DefaultErrorHandler(recoverer, backOff);
}
```

### Implementing `ModuleIdExtractor`

Provide a bean that extracts the module identifier from an incoming `ResourceEvent`. The string
is included in the `ResourceResultEvent` sent to the confirmation topic:

The extraction logic is event-specific. Return `null` when the module identifier is not applicable;
the confirmation is still sent with a `null` `moduleId` field.

### Transactional mode

By default, event listeners fire immediately on publication (non-transactional). To fire only after
a database transaction commits, set:

```yaml
application:
  event-confirmation:
    success-listener:
      transactional: true
      transaction-phase: AFTER_COMMIT   # default when transactional=true
    failure-listener:
      transactional: true
```

### Custom sender

To replace the default Kafka-backed sender, register a bean named `eventConfirmationSender`:

```java
@Bean("eventConfirmationSender")
public EventConfirmationSender myCustomSender() {
    return new MyCustomEventConfirmationSender();
}
```

When this bean is present, `EventConfirmationConfiguration` skips creating the default
`KafkaEventConfirmationSender` and its thread-pool executor.

---

## Full Configuration Reference

| Property                                                                          | Type      | Default  | Description                                             |
|:----------------------------------------------------------------------------------|:----------|:---------|:--------------------------------------------------------|
| `application.kafka.consumer.listener.<name>.topic-pattern`                        | `String`  | —        | Regex topic-name pattern for the named listener         |
| `application.kafka.consumer.listener.<name>.group-id`                             | `String`  | —        | Consumer group ID for the named listener                |
| `application.kafka.consumer.listener.<name>.concurrency`                          | `Integer` | `1`      | Number of concurrent consumer threads for the listener  |
| `application.event-confirmation.enabled`                                          | `boolean` | `false`  | Activate the event-confirmation subsystem               |
| `application.event-confirmation.topic`                                            | `String`  | —        | Kafka topic for `ResourceResultEvent` confirmations; must not be blank when enabled |
| `application.event-confirmation.success-listener.transactional`                   | `boolean` | `false`  | Fire the success listener inside a transaction synchronisation callback |
| `application.event-confirmation.success-listener.transaction-phase`               | `String`  | `AFTER_COMMIT` | Transaction phase for the transactional success listener |
| `application.event-confirmation.failure-listener.transactional`                   | `boolean` | `false`  | Fire the failure listener inside a transaction synchronisation callback |
| `application.event-confirmation.failure-listener.transaction-phase`               | `String`  | `AFTER_COMMIT` | Transaction phase for the transactional failure listener |
