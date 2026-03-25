# folio-backend-common

Shared foundation library for FOLIO backend Spring Boot modules. Provides the core domain model,
error response types, validation constraints, pagination utilities, and common helper classes used
across all FOLIO services.

## Table of Contents

- [Domain Model](#domain-model)
- [Error Model](#error-model)
- [Validation](#validation)
- [Configuration](#configuration)
- [Pagination](#pagination)
- [Utilities](#utilities)

---

## Domain Model

Java representation of the Okapi/FOLIO module registry. All model classes use fluent setters and
Jackson annotations for seamless JSON serialization.

### Application and Module Descriptors

| Class                   | Description                                                                                                                                                                                          |
|:------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ApplicationDescriptor` | Top-level descriptor for a FOLIO application. Contains module references, full module descriptors, cross-application dependencies, deployment configuration, and arbitrary metadata.                 |
| `ModuleDescriptor`      | Full Okapi-compatible module descriptor with provided/required interfaces, routing entries, permission sets, environment declarations, and launch/UI descriptor sections.                            |
| `Module`                | Lightweight module reference by `name` and `version`, with optional `id` and `url`.                                                                                                                  |
| `InterfaceDescriptor`   | Describes a single Okapi interface provided by a module. Includes `id`, `version`, `interfaceType`, handler routing entries, and permission sets. Provides `isCompatible()` and `compare()` methods. |
| `InterfaceReference`    | Lightweight reference to a required/optional interface by `id` and `version`.                                                                                                                        |
| `Dependency`            | Cross-application dependency with semver range validation via `@SemVersionOrRange`.                                                                                                                  |

### Routing and Permissions

| Class                  | Description                                                                                                |
|:-----------------------|:-----------------------------------------------------------------------------------------------------------|
| `RoutingEntry`         | HTTP routing rule: HTTP methods, path pattern, phase, level, required/desired permissions, timer schedule. |
| `RoutingEntrySchedule` | Cron-based schedule for timer routing entries: `cron` expression and `zone`.                               |
| `Permission`           | FOLIO permission definition: name, sub-permissions, display name, and visibility flag.                     |
| `Capability`           | Authorization capability with resource, action, type, and scope-to-capability mapping.                     |

### Deployment and Metadata

| Class                   | Description                                                                                                           |
|:------------------------|:----------------------------------------------------------------------------------------------------------------------|
| `ApplicationDeployment` | Deployment section of `ApplicationDescriptor`: module lists and `Deployment` object.                                  |
| `Deployment`            | Deployment technology type and chart reference (e.g. a Helm chart).                                                   |
| `LaunchDescriptor`      | Module launch configuration: Docker image, command, environment variables, wait settings.                             |
| `UiModuleDescriptor`    | UI module deployment info: npm reference, URL, args.                                                                  |
| `EnvEntry`              | Single environment variable declaration with name, value, and description.                                            |
| `AnyDescriptor`         | Arbitrary JSON object container using `@JsonAnyGetter`/`@JsonAnySetter`. Used for `metadata` and `extensions` fields. |

### Response Wrappers

| Class             | Description                                                                                                                     |
|:------------------|:--------------------------------------------------------------------------------------------------------------------------------|
| `ResultList<T>`   | Paginated API response wrapper with `totalRecords` and a named records array. Factory methods: `asSinglePage(list)`, `empty()`. |
| `SearchResult<T>` | Internal search result wrapper with `totalRecords` and `records`. Factory methods: `of(list)`, `empty()`.                       |

---

## Error Model

Standardized error response structure consistent across all FOLIO services.

| Class           | Description                                                                                                                                                   |
|:----------------|:--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `ErrorResponse` | Top-level error body containing `List<Error> errors` and `totalRecords`.                                                                                      |
| `Error`         | Single error entry with `message`, `type` (exception class name), `ErrorCode`, and `List<Parameter>`.                                                         |
| `Parameter`     | Key-value pair providing additional context for an error (e.g. field name and invalid value).                                                                 |
| `ErrorCode`     | Enum of canonical error codes: `UNKNOWN_ERROR`, `SERVICE_ERROR`, `VALIDATION_ERROR`, `NOT_FOUND_ERROR`, `FOUND_ERROR`, `ROUTE_NOT_FOUND_ERROR`, `AUTH_ERROR`. |

---

## Validation

### `@SemVersionOrRange`

A Jakarta Bean Validation constraint that verifies a `String` field or parameter is either a valid
semantic version (`1.2.3`) or a valid semver range expression (`>=1.0.0 <2.0.0`). Backed by the
`semver4j` library.

```java
public class Dependency {
    @SemVersionOrRange
    private String version;
}
```

Default violation message: `Invalid semantic version or range(s): "${validatedValue}"`

---

## Configuration

### `FolioEnvironment`

Spring `@Configuration` bean that binds the `application.environment` property. The environment
name is validated non-empty and must match the pattern `[\w\-]+`. It is used as a prefix for Kafka
topic names across all modules.

```yaml
application:
  environment: trillium   # e.g., "trillium", "folio-dev"
```

**Static resolution without Spring context:**

```java
// Checks ENV env var, then "env" system property, then defaults to "folio"
String env = FolioEnvironment.getFolioEnvName();
```

---

## Pagination

### `OffsetRequest`

A Spring Data `Pageable` implementation that uses raw offset/limit instead of page-number/size.
Default sort is ascending by `id`.

```java
Pageable page = OffsetRequest.of(0, 100);
Pageable page = OffsetRequest.of(50, 25, Sort.by("name"));
```

### `PaginationUtils`

Batch-loading utilities for FOLIO API clients.

| Method                                   | Description                                         |
|:-----------------------------------------|:----------------------------------------------------|
| `loadInBatches(ids, queryFn, batchSize)` | Splits ID list into batches and accumulates results |
| `subListAtOffset(offset, limit, list)`   | Safe sublist extraction by offset and limit         |

---

## Utilities

### `OkapiHeaders`

Constants for all `x-okapi-*` HTTP header names used across FOLIO services.

```java
String tenant = request.getHeader(OkapiHeaders.TENANT);    // "x-okapi-tenant"
String token  = request.getHeader(OkapiHeaders.TOKEN);     // "x-okapi-token"
String userId = request.getHeader(OkapiHeaders.USER_ID);   // "x-okapi-user-id"
```

### `SemverUtils`

Semantic version parsing for FOLIO artifact IDs of the form `name-X.Y.Z`.

| Method                                  | Description                                               |
|:----------------------------------------|:----------------------------------------------------------|
| `getName(sourceId)`                     | Strips the semver suffix: `"mod-foo-1.2.3"` → `"mod-foo"` |
| `getVersion(sourceId)`                  | Returns the version suffix: `"mod-foo-1.2.3"` → `"1.2.3"` |
| `getNames(ids)`                         | Maps and deduplicates names from a collection of IDs      |
| `satisfies(version, range)`             | Tests a bare version string against a semver range        |
| `applicationSatisfies(sourceId, range)` | Extracts version from a compound ID, then checks range    |

### `InterfaceComparisonUtils`

Okapi-style interface version compatibility checks using the `XX.YY` / `XX.YY.ZZ` version format.

| Method                           | Description                                                                           |
|:---------------------------------|:--------------------------------------------------------------------------------------|
| `isCompatible(id1, v1, id2, v2)` | True when IDs match and provided version satisfies required                           |
| `compare(id1, v1, id2, v2)`      | Returns `0` (equal), `±1` (patch diff), `±2` (minor diff), `MAX_VALUE` (incompatible) |

### `CqlQuery`

Type-safe builder for CQL (Contextual Query Language) query strings.

```java
String cql = CqlQuery.exactMatchAny("id", List.of("uuid-1", "uuid-2")).toString();
// id==("uuid-1" or "uuid-2")
```

### `CollectionUtils`

Null-safe, stream-oriented collection helpers.

| Method                          | Description                                                         |
|:--------------------------------|:--------------------------------------------------------------------|
| `toStream(collection)`          | Null-safe stream from a collection                                  |
| `mapItems(collection, fn)`      | Map to list                                                         |
| `mapItemsToSet(collection, fn)` | Map to set                                                          |
| `reverseList(list)`             | Reverse into a new list (null-safe)                                 |
| `takeOne(collection)`           | Enforces single-element semantics; throws on 0 or 2+ elements       |
| `findOne(collection)`           | Returns `Optional` only when the collection has exactly one element |

### `ExceptionHandlerUtils`

Factory helpers for standardized `ErrorResponse` and `ResponseEntity<ErrorResponse>` objects for
use in Spring `@ExceptionHandler` methods.

| Method                                       | Description                                         |
|:---------------------------------------------|:----------------------------------------------------|
| `buildValidationError(ex, params)`           | Builds `ErrorResponse` with `VALIDATION_ERROR` code |
| `buildErrorResponse(ex, params, code)`       | Generic error response builder                      |
| `buildResponseEntity(ex, status, code)`      | Wraps an exception into an HTTP response            |
| `buildResponseEntity(errorResponse, status)` | Wraps a pre-built error response                    |

### Other Utilities

| Class                     | Description                                                                                |
|:--------------------------|:-------------------------------------------------------------------------------------------|
| `Collectors`              | `toLinkedHashMap(keyFn)` and `toLinkedHashMap(keyFn, valueFn)` stream collectors           |
| `KeycloakPermissionUtils` | Formats Keycloak permission name strings: `"[scopes] access for 'policy' to 'resource'"`   |
| `UuidUtils`               | `randomId()` — returns a new random UUID as a string                                       |
| `TransactionHelper`       | Spring component to execute a `Supplier<T>` inside a programmatic `@Transactional` context |
