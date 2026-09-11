# folio-kafka-consumer

Spring Boot consumer library for FOLIO applications. Provides tenant-aware Kafka message filtering,
per-listener topic and group-ID configuration, automatic module metadata resolution, and event
confirmation support for async entitlement feedback — so only messages intended for entitled
tenants are delivered to the listener, and callers can receive a result event when processing
completes or fails.

## Table of Contents

- [Activation](#activation)
- [Listener Configuration](#listener-configuration)
- [Tenant-Aware Filtering](#tenant-aware-filtering)
- [Module Metadata Resolution](#module-metadata-resolution)
- [Using the Filter in a Listener Container Factory](#using-the-filter-in-a-listener-container-factory)
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

This annotation imports three configurations:

| Imported configuration               | Registers                                                                                                        |
|:-------------------------------------|:-----------------------------------------------------------------------------------------------------------------|
| `KafkaConsumerFilteringConfiguration` | `tenantAwareMessageFilter` bean (active filter or no-op pass-through, depending on properties)                  |
| `ModuleMetadataConfiguration`         | `moduleMetadata` bean resolved from the application's name and version                                          |
| `KafkaConsumerPropertiesConfiguration`| `kafkaConsumerProperties` bean bound to `application.kafka.consumer.*` with a stable, SpEL-friendly bean name  |

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

## Tenant-Aware Filtering

By default, the `tenantAwareMessageFilter` bean is a no-op pass-through that accepts all records.
Set `application.kafka.consumer.filtering.tenant-filter.enabled=true` to activate real filtering.

### How filtering works

When enabled, `EnabledTenantMessageFilter` intercepts every incoming record:

1. Calls `getTenant()` on the `TenantAwareEvent` payload. If the value is blank or `null`, the
   record is accepted immediately and a warning is logged — no entitlement check is performed.
2. Calls the tenant-entitlement service to fetch the set of tenants currently entitled for this
   module (result is cached per poll cycle).
3. If the entitled set is **non-empty** but does not contain the record's tenant, the
   `tenantDisabledStrategy` is applied.
4. If the entitled set is **empty** (no tenant is entitled at all), the
   `allTenantsDisabledStrategy` is applied.

### Strategies

| Value    | Effect when a tenant is not entitled                                                         |
|:---------|:---------------------------------------------------------------------------------------------|
| `ACCEPT` | Accept the record and deliver it to the listener                                             |
| `SKIP`   | Silently discard the record (default for `tenantDisabledStrategy`)                           |
| `FAIL`   | Throw `TenantIsDisabledException` or `TenantsAreDisabledException` (default for `allTenantsDisabledStrategy`) |

### Tenant entitlement service

When `tenant-filter.enabled=true`, the library creates and configures the `TenantEntitlementClient`
automatically. The consuming application only needs to supply the base URL via the `okapi.url`
property — no manually created beans are required.

```yaml
okapi:
  url: http://mgr-tenant-entitlements:8080
```

If the `loggingInterceptor` bean from `folio-spring-support` is present on the classpath, it is
picked up automatically and applied to every request made by the entitlement client.

### Filtering configuration

```yaml
application:
  kafka:
    consumer:
      filtering:
        tenant-filter:
          enabled: true
          ignore-empty-batch: true          # default: true
          tenant-disabled-strategy: SKIP    # default: SKIP
          all-tenants-disabled-strategy: FAIL  # default: FAIL
```

---

## Module Metadata Resolution

`ModuleMetadataConfiguration` resolves the current module's name and version through an ordered
chain of providers. The first provider that succeeds determines the result.

| Priority | Provider                             | Source                                                                              | Condition                              |
|:---------|:-------------------------------------|:------------------------------------------------------------------------------------|:---------------------------------------|
| 1        | `AppPropertiesModuleDataProvider`    | `spring.application.name` and `spring.application.version` properties              | Always registered                      |
| 10       | `BuildPropertiesModuleDataProvider`  | `build.artifact` / `build.version` from `META-INF/build-info.properties`           | Only when `BuildProperties` bean exists |
| 20       | `ManifestModuleDataProvider`         | `Implementation-Title` / `Implementation-Version` in `META-INF/MANIFEST.MF`        | Always registered                      |
| 30       | `PomModuleDataProvider`              | `artifactId` / `version` in `META-INF/maven/<groupId>/<artifactId>/pom.properties` | Always registered                      |
| 40       | `ModulePropertiesModuleDataProvider` | `module.name` / `module.version` in `classpath:module.properties`                  | Always registered                      |

The location of `module.properties` can be overridden:

```yaml
spring:
  application:
    module-properties:
      location: classpath:custom/module.properties
```

To supply the module name and version explicitly (recommended), set the Spring properties:

```yaml
spring:
  application:
    name: mod-my-service
    version: 1.2.3
```

### Custom provider

Register a bean named `moduleDataProvider` to replace the default `CompositeModuleDataProvider`:

```java
@Bean("moduleDataProvider")
public ModuleDataProvider moduleDataProvider() {
    return () -> new ModuleData("mod-my-service", "1.2.3");
}
```

---

## Using the Filter in a Listener Container Factory

Wire the `tenantAwareMessageFilter` bean into the Kafka listener container factory:

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, ResourceEvent<?>> kafkaListenerContainerFactory(
        ConsumerFactory<String, ResourceEvent<?>> consumerFactory,
        RecordFilterStrategy<String, ResourceEvent<?>> tenantAwareMessageFilter) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, ResourceEvent<?>>();
    factory.setConsumerFactory(consumerFactory);
    factory.setRecordFilterStrategy(tenantAwareMessageFilter);
    return factory;
}
```

The `tenantAwareMessageFilter` bean is `@ConditionalOnMissingBean(name = "tenantAwareMessageFilter")`,
so a custom `RecordFilterStrategy` bean with that name takes precedence.

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

```java
@Bean
public ModuleIdExtractor moduleIdExtractor(ModuleMetadata moduleMetadata) {
    return event -> moduleMetadata.getId();
}
```

Return `null` when the module identifier is not applicable; the confirmation is still sent with a
`null` `moduleId` field.

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
| `application.kafka.consumer.filtering.tenant-filter.enabled`                      | `boolean` | `false`  | Activate real tenant-entitlement filtering              |
| `application.kafka.consumer.filtering.tenant-filter.ignore-empty-batch`           | `boolean` | `true`   | Signal Kafka to skip delivery on empty poll batches     |
| `application.kafka.consumer.filtering.tenant-filter.tenant-disabled-strategy`     | `String`  | `SKIP`   | Strategy when a record's tenant is not entitled         |
| `application.kafka.consumer.filtering.tenant-filter.all-tenants-disabled-strategy`| `String`  | `FAIL`   | Strategy when no tenants at all are entitled            |
| `spring.application.module-properties.location`                                   | `String`  | `classpath:module.properties` | Override path for `module.properties` |
| `application.event-confirmation.enabled`                                          | `boolean` | `false`  | Activate the event-confirmation subsystem               |
| `application.event-confirmation.topic`                                            | `String`  | —        | Kafka topic for `ResourceResultEvent` confirmations; must not be blank when enabled |
| `application.event-confirmation.success-listener.transactional`                   | `boolean` | `false`  | Fire the success listener inside a transaction synchronisation callback |
| `application.event-confirmation.success-listener.transaction-phase`               | `String`  | `AFTER_COMMIT` | Transaction phase for the transactional success listener |
| `application.event-confirmation.failure-listener.transactional`                   | `boolean` | `false`  | Fire the failure listener inside a transaction synchronisation callback |
| `application.event-confirmation.failure-listener.transaction-phase`               | `String`  | `AFTER_COMMIT` | Transaction phase for the transactional failure listener |
