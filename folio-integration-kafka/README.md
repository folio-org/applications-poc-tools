# folio-integration-kafka

Spring Boot Kafka integration library for FOLIO applications, split into three focused sub-modules.

## Sub-modules

| Module                                                    | Description                                                                                                        |
|:----------------------------------------------------------|:-------------------------------------------------------------------------------------------------------------------|
| [folio-kafka-common](folio-kafka-common/README.md)        | Shared domain model: `ResourceEvent<T>` envelope and `ResourceEventType` enum used by producers and consumers      |
| [folio-kafka-producer](folio-kafka-producer/README.md)    | Producer infrastructure: declarative topic creation at startup, programmatic lifecycle, and multi-tenant naming    |
| [folio-kafka-consumer](folio-kafka-consumer/README.md)    | Consumer infrastructure: tenant-aware message filtering, per-listener configuration, and module metadata resolution |

---

## Quick Start

### Producer

```java
@Configuration
@EnableKafkaProducer
public class KafkaConfiguration { }
```

```yaml
application:
  environment: trillium
  kafka:
    producer:
      topics:
        - name: my-module.events
          num-partitions: 10
```

See [folio-kafka-producer/README.md](folio-kafka-producer/README.md) for the full configuration
reference and programmatic topic management API.

### Consumer

```java
@Configuration
@EnableKafkaConsumer
public class KafkaConfiguration { }
```

```yaml
spring:
  application:
    name: mod-my-service
    version: 1.2.3
application:
  kafka:
    consumer:
      listener:
        events:
          topic-pattern: trillium\..*\.my-module\.events
          group-id: mod-my-service-group
```

See [folio-kafka-consumer/README.md](folio-kafka-consumer/README.md) for the tenant filtering
configuration and module metadata resolution details.
