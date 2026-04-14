# folio-kafka-common

Shared Kafka domain model for FOLIO services. Provides the generic event envelope and lifecycle
operation enum used by both Kafka producers and consumers across all FOLIO modules.

## Table of Contents

- [Event Model](#event-model)
- [Tenant-Aware Contract](#tenant-aware-contract)
- [Event Types](#event-types)

---

## Event Model

### `ResourceEvent<T>`

A generic Kafka event envelope carrying metadata about a resource change and an optional payload.
Serialized to and from JSON using Jackson.

| Field          | JSON key       | Nullable | Description                                                         |
|:---------------|:---------------|:---------|:--------------------------------------------------------------------|
| `id`           | `id`           | —        | Resource identifier                                                 |
| `type`         | `type`         | —        | Lifecycle operation (`CREATE`, `UPDATE`, `DELETE`, `DELETE_ALL`)    |
| `tenant`       | `tenant`       | —        | Tenant identifier (FOLIO tenant name)                               |
| `resourceName` | `resourceName` | —        | Human-readable resource name                                        |
| `newValue`     | `new`          | yes      | New state of the resource; present for `CREATE` and `UPDATE`        |
| `oldValue`     | `old`          | yes      | Previous state of the resource; present for `UPDATE` and `DELETE`   |

`newValue` and `oldValue` are both nullable: a `CREATE` event carries only `newValue`, a `DELETE`
event carries only `oldValue`, and an `UPDATE` event may carry both.

```java
ResourceEvent<Item> event = ResourceEvent.<Item>builder()
    .id(item.getId())
    .type(ResourceEventType.CREATE)
    .tenant("diku")
    .resourceName("Item")
    .newValue(item)
    .build();
```

---

## Tenant-Aware Contract

### `TenantAwareEvent`

A lightweight interface for event payloads that carry a tenant identifier. Implement it to make a
custom event type work with the consumer-side tenant filter without depending on `ResourceEvent`.

```java
public interface TenantAwareEvent {
    @Nullable String getTenant();
}
```

`ResourceEvent<T>` implements this interface out of the box, so no change is required for existing
listeners that already use `ResourceEvent`.

---

## Event Types

`ResourceEventType` is serialized to JSON by its string value (via `@JsonValue`).

| Constant     | JSON value     | Description                                      |
|:-------------|:---------------|:-------------------------------------------------|
| `CREATE`     | `"CREATE"`     | A new resource instance was created              |
| `UPDATE`     | `"UPDATE"`     | An existing resource instance was modified       |
| `DELETE`     | `"DELETE"`     | A single resource instance was deleted           |
| `DELETE_ALL` | `"DELETE_ALL"` | All instances of a resource were deleted in bulk |
