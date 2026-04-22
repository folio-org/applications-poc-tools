# folio-integration-kafka

Spring Kafka integration library providing topic management, admin operations, and tenant-aware consumer message filtering.

## Features

- **Topic management** -- declarative topic creation from application properties
- **Admin operations** -- create, delete, and find topics via `KafkaAdminService`
- **Tenant-aware filtering** -- filter Kafka consumer messages based on tenant entitlements
- **Module metadata discovery** -- automatic module ID detection from multiple sources

## Topic Management

### Setup

Add `@EnableKafka` to your configuration class:

```java
@EnableKafka
@SpringBootApplication
public class MyApplication { }
```

This imports `KafkaTopicConfiguration`, `KafkaAdminService`, and binds `FolioKafkaProperties`.

### Configuration

```yaml
application:
  kafka:
    topics:
      - name: my-topic
        numPartitions: 10
        replicationFactor: 1
```

### Key Classes

- `KafkaAdminService` -- service for creating, deleting, and finding topics
- `KafkaUtils` -- static helpers for building topic names

## Tenant-Aware Consumer Filtering

### Setup

Add `@EnableKafkaConsumer` to your configuration class:

```java
@EnableKafkaConsumer
@SpringBootApplication
public class MyApplication { }
```

This activates the full consumer infrastructure: filtering configuration, consumer properties, and module metadata detection.

### Configuration

```yaml
application:
  kafka:
    consumer:
      filtering:
        tenant-filter:
          enabled: true
          ignore-empty-batch: true
          tenant-disabled-strategy: SKIP    # ACCEPT, SKIP, or FAIL
          all-tenants-disabled-strategy: FAIL  # ACCEPT, SKIP, or FAIL
      listener:
        my-listener:
          topic-pattern: "my-topic-.*"
          group-id: my-group
          concurrency: 3

okapi:
  url: http://localhost:8081   # base URL for the entitlement service
```

### How It Works

The `EnabledTenantMessageFilter` implements Spring Kafka's `RecordFilterStrategy`. For each incoming record:

1. If the tenant is blank, the record is **accepted**
2. The filter queries the tenant entitlement service for tenants entitled to the current module
3. If the tenant is entitled, the record is **accepted**
4. If not entitled, the configured `tenant-disabled-strategy` applies (ACCEPT/SKIP/FAIL)
5. If no tenants are entitled at all, the `all-tenants-disabled-strategy` applies

Use the filter in your `@KafkaListener`:

```java
@KafkaListener(topics = "my-topic", filter = "tenantAwareMessageFilter")
public void handle(ConsumerRecord<String, MyEvent> record) { }
```

### Module Metadata Discovery

The module ID (e.g., `mod-users-1.0.0`) is detected automatically using a chain of providers (first match wins):

1. `spring.application.name` + `spring.application.version` properties
2. Spring Boot `BuildProperties` (from `build-info.properties`)
3. `META-INF/MANIFEST.MF`
4. `META-INF/maven/**/pom.properties`
5. Custom `module.properties` on the classpath

### Event Model

- `TenantAwareEvent` -- interface for events carrying a tenant identifier
- `ResourceEvent<T>` -- generic event envelope with `id`, `type`, `tenant`, `resourceName`, `newValue`, `oldValue`
- `ResourceEventType` -- enum: `CREATE`, `UPDATE`, `DELETE`, `DELETE_ALL`

## Usage

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-integration-kafka</artifactId>
  <version>${applications-poc-tools.version}</version>
</dependency>
```
