# folio-integration-kafka

Spring Boot integration library that standardizes Kafka topic management across FOLIO modules.
Provides declarative topic creation at startup, programmatic topic lifecycle management, and
consistent multi-tenant topic naming conventions.

## Table of Contents

- [Activation](#activation)
- [Configuration](#configuration)
- [Topic Naming](#topic-naming)
- [Programmatic Topic Management](#programmatic-topic-management)

---

## Activation

Add `@EnableKafka` to a Spring `@Configuration` class to activate all Kafka topic management
infrastructure. This single annotation imports `KafkaTopicConfiguration`, `FolioEnvironment`, and
`KafkaAdminService`, and enables binding of `FolioKafkaProperties`.

```java
@Configuration
@EnableKafka
public class MyAppConfiguration { }
```

> **Note:** This annotation conflicts with Spring Kafka's own `@EnableKafka`. Import the FOLIO one
> explicitly: `org.folio.integration.kafka.EnableKafka`.

---

## Configuration

### Environment name

The environment name is a required prefix applied to every Kafka topic. It is resolved from
`folio-backend-common`'s `FolioEnvironment` in the following order:

1. Environment variable `ENV`
2. JVM system property `env`
3. Default: `folio`

When using Spring context, the name can also be bound via:

```yaml
application:
  environment: trillium   # must match [\w\-]+
```

### Kafka topics

Declare topics to be created automatically at application startup:

```yaml
application:
  kafka:
    topics:
      - name: my-module.events           # logical name (env prefix is added automatically)
        num-partitions: 10               # optional, null = broker default
        replication-factor: -1           # optional, null/-1 = broker default
      - name: my-module.notifications
        num-partitions: 3
        replication-factor: 3
```

At startup, `KafkaTopicConfiguration` iterates this list, prefixes each name with the environment
(e.g. `trillium.my-module.events`), and creates any topics that do not already exist on the broker.
Topic creation is idempotent.

### Full configuration reference

| Property                                         | Type      | Default        | Description                                   |
|:-------------------------------------------------|:----------|:---------------|:----------------------------------------------|
| `application.environment`                        | `String`  | —              | Environment name; validated against `[\w\-]+` |
| `application.kafka.topics[n].name`               | `String`  | —              | Logical topic name without env prefix         |
| `application.kafka.topics[n].num-partitions`     | `Integer` | broker default | Number of partitions                          |
| `application.kafka.topics[n].replication-factor` | `Short`   | broker default | Replication factor; `-1` uses broker default  |

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
