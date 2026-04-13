# folio-kafka-producer

Spring Boot producer library for FOLIO applications. Provides declarative Kafka topic creation at
startup, programmatic topic lifecycle management, and consistent multi-tenant topic naming
conventions.

## Table of Contents

- [Activation](#activation)
- [Configuration](#configuration)
- [Topic Naming](#topic-naming)
- [Programmatic Topic Management](#programmatic-topic-management)

---

## Activation

Add `@EnableKafkaProducer` to a Spring `@Configuration` class to activate the Kafka producer
infrastructure:

```java
@Configuration
@EnableKafkaProducer
public class KafkaConfiguration { }
```

This annotation imports `KafkaTopicConfiguration`, `FolioEnvironment`, and `KafkaAdminService`,
and enables binding of `KafkaProducerProperties` from `application.kafka.producer.*`.

---

## Configuration

### Environment name

The environment name is a required prefix applied to every Kafka topic. It is resolved by
`FolioEnvironment` (from `folio-backend-common`) in the following order:

1. Environment variable `ENV`
2. JVM system property `env`
3. Default: `folio`

It can also be set via Spring configuration:

```yaml
application:
  environment: trillium   # must match [\w\-]+
```

### Kafka topics

Declare topics to be created automatically at application startup:

```yaml
application:
  kafka:
    producer:
      topics:
        - name: my-module.events           # logical name (env prefix is added automatically)
          num-partitions: 10               # optional, null = broker default
          replication-factor: -1           # optional, -1 = broker default
        - name: my-module.notifications
          num-partitions: 3
          replication-factor: 3
```

At startup, `KafkaTopicConfiguration` iterates this list, prefixes each name with the environment
(e.g. `trillium.my-module.events`), and creates any topics that do not already exist on the broker.
Topic creation is idempotent.

### Full configuration reference

| Property                                                        | Type      | Default        | Description                                   |
|:----------------------------------------------------------------|:----------|:---------------|:----------------------------------------------|
| `application.environment`                                       | `String`  | —              | Environment name; validated against `[\w\-]+` |
| `application.kafka.producer.topics[n].name`                     | `String`  | —              | Logical topic name without env prefix         |
| `application.kafka.producer.topics[n].num-partitions`           | `Integer` | broker default | Number of partitions                          |
| `application.kafka.producer.topics[n].replication-factor`       | `Short`   | broker default | Replication factor; `-1` uses broker default  |

---

## Topic Naming

`KafkaUtils` provides static helpers that enforce consistent naming conventions across all FOLIO
modules.

### Environment-scoped topics

Shared by all tenants. Used for topics that are not tenant-specific.

```java
// Result: "trillium.inventory.items"
String topic = KafkaUtils.getEnvTopicName("inventory.items");
```

### Tenant-scoped topics

Isolated per tenant. Used for topics that carry tenant-specific data.

```java
// Result: "trillium.diku.inventory.items"
String topic = KafkaUtils.getTenantTopicName("inventory.items", "diku");
```

The `{env}` prefix is resolved from the `ENV` environment variable, the `env` system property, or
the default `"folio"`.

---

## Programmatic Topic Management

`KafkaAdminService` exposes runtime topic lifecycle operations, typically used during tenant
onboarding and offboarding.

### Create a topic

```java
@RequiredArgsConstructor
public class TenantService {
    private final KafkaAdminService kafkaAdminService;

    public void onboardTenant(String tenantId) {
        String topicName = KafkaUtils.getTenantTopicName("my-module.events", tenantId);
        NewTopic topic = KafkaUtils.createTopic(topicName, 10, (short) -1);
        kafkaAdminService.createTopic(topic);
    }
}
```

### Delete topics

```java
kafkaAdminService.deleteTopics(List.of(
    KafkaUtils.getTenantTopicName("my-module.events", tenantId)
));
```

### Check which topics exist

```java
Collection<String> existing = kafkaAdminService.findTopics(List.of(
    KafkaUtils.getTenantTopicName("my-module.events", "diku"),
    KafkaUtils.getTenantTopicName("my-module.events", "tenant2")
));
```

`findTopics` returns only the subset of the requested names that actually exist on the broker.
All three methods throw `KafkaException` on broker communication failures.
