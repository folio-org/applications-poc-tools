# folio-kafka-consumer

Spring Boot consumer library for FOLIO applications. Provides tenant-aware Kafka message filtering,
per-listener topic and group-ID configuration, and automatic module metadata resolution — so only
messages intended for entitled tenants are delivered to the listener.

## Table of Contents

- [Activation](#activation)
- [Listener Configuration](#listener-configuration)
- [Tenant-Aware Filtering](#tenant-aware-filtering)
- [Module Metadata Resolution](#module-metadata-resolution)
- [Using the Filter in a Listener Container Factory](#using-the-filter-in-a-listener-container-factory)
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

1. Extracts the `tenant` field from the `ResourceEvent` payload.
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

When `tenant-filter.enabled=true`, the application context must expose an
`HttpServiceProxyFactory` bean configured with the base URL of the tenant-entitlement service.
The library creates the `TenantEntitlementClient` from it automatically.

```java
@Bean
public HttpServiceProxyFactory httpServiceProxyFactory(RestClient.Builder restClientBuilder) {
    RestClient restClient = restClientBuilder
        .baseUrl("http://mgr-tenant-entitlements:8080")
        .build();
    return HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(restClient))
        .build();
}
```

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

## Full Configuration Reference

| Property                                                                          | Type      | Default  | Description                                             |
|:----------------------------------------------------------------------------------|:----------|:---------|:--------------------------------------------------------|
| `application.kafka.consumer.listener.<name>.topic-pattern`                        | `String`  | —        | Regex topic-name pattern for the named listener         |
| `application.kafka.consumer.listener.<name>.group-id`                             | `String`  | —        | Consumer group ID for the named listener                |
| `application.kafka.consumer.filtering.tenant-filter.enabled`                      | `boolean` | `false`  | Activate real tenant-entitlement filtering              |
| `application.kafka.consumer.filtering.tenant-filter.ignore-empty-batch`           | `boolean` | `true`   | Signal Kafka to skip delivery on empty poll batches     |
| `application.kafka.consumer.filtering.tenant-filter.tenant-disabled-strategy`     | `String`  | `SKIP`   | Strategy when a record's tenant is not entitled         |
| `application.kafka.consumer.filtering.tenant-filter.all-tenants-disabled-strategy`| `String`  | `FAIL`   | Strategy when no tenants at all are entitled            |
| `spring.application.module-properties.location`                                   | `String`  | `classpath:module.properties` | Override path for `module.properties` |
