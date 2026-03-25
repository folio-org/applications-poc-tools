# folio-backend-testing

Shared testing library for FOLIO backend Spring Boot modules. Provides reusable JUnit 5 extensions,
Testcontainers wrappers, WireMock support, Kafka utilities, JWT generation, and MockMvc base classes.

All dependencies are published at `compile` scope so downstream modules can consume the full testing
infrastructure without additional scope configuration.

## Table of Contents

- [Environment Variables](#environment-variables)
- [Annotations](#annotations)
- [Extensions and Listeners](#extensions-and-listeners)
- [Utility Classes](#utility-classes)
- [Base Test Class](#base-test-class)
- [WireMock Integration](#wiremock-integration)
- [Keycloak Integration](#keycloak-integration)
- [Kafka Integration](#kafka-integration)

---

## Environment Variables

All Testcontainers images can be overridden via environment variables. This is the primary way to
point tests at images from a private Docker registry or to use a newer image version without
changing source code.

| Environment variable                | Default image                           | Container  |
|:------------------------------------|:----------------------------------------|------------|
| `TESTCONTAINERS_POSTGRES_IMAGE`     | `postgres:16-alpine`                    | PostgreSQL |
| `TESTCONTAINERS_KAFKA_IMAGE`        | `apache/kafka-native:3.8.0`             | Kafka      |
| `TESTCONTAINERS_KEYCLOAK_IMAGE`     | `quay.io/keycloak/keycloak:26.5.2`      | Keycloak   |
| `TESTCONTAINERS_WIREMOCK_IMAGE`     | `wiremock/3.13.2-2-alpine`              | WireMock   |

Example — redirect all containers to a private registry:

```shell
export TESTCONTAINERS_POSTGRES_IMAGE=registry.example.com/postgres:16-alpine
export TESTCONTAINERS_KAFKA_IMAGE=registry.example.com/apache/kafka-native:3.8.0
export TESTCONTAINERS_KEYCLOAK_IMAGE=registry.example.com/keycloak/keycloak:26.5.2
export TESTCONTAINERS_WIREMOCK_IMAGE=registry.example.com/wiremock:3.13.2-2-alpine
```

Image resolution is handled by `DockerImageRegistry`. Each `getXxxImageName()` method reads the
corresponding environment variable and falls back to the default when the variable is absent.

---

## Annotations

### Container lifecycle

Annotate a test class with one or more of these to start the required Docker containers. Each
container is a static singleton shared across all test classes in the same JVM run.

| Annotation               | Container started  | System property set                                                                                                       |
|:-------------------------|:-------------------|:--------------------------------------------------------------------------------------------------------------------------|
| `@EnablePostgres`        | PostgreSQL         | `spring.datasource.url`                                                                                                   |
| `@EnableKafka`           | Kafka              | `spring.kafka.bootstrap-servers`                                                                                          |
| `@EnableKeycloakTlsMode` | Keycloak (TLS)     | `KC_URL`, `KC_ADMIN_CLIENT_ID`, `KC_ADMIN_CLIENT_SECRET`, `KC_ADMIN_USERNAME`, `KC_ADMIN_PASSWORD`, `KC_ADMIN_GRANT_TYPE` |
| `@EnableWireMock`        | WireMock           | `wm.url` and any properties listed in `wiremock-url.vars`                                                                 |

### Security profiles

These annotations activate Spring test properties only — they do not start any containers.
Combine them with the container annotations above.

| Annotation                   | Activated properties                                                                                                          |
|:-----------------------------|:------------------------------------------------------------------------------------------------------------------------------|
| `@EnableKeycloakSecurity`    | `application.keycloak.enabled=true`, `application.security.enabled=true`, `application.secret-store.environment=folio`        |
| `@EnableKeycloakDataImport`  | `application.keycloak.enabled=true`, `application.keycloak.import.enabled=true`, `application.secure-store.environment=folio` |
| `@EnableOkapiSecurity`       | `application.okapi.enabled=true`, `application.keycloak.enabled=false`, `application.security.enabled=true`                   |

### Test data

Both annotations are repeatable and can be placed at class level (applied to every test method)
or at method level (applied to that test only).

| Annotation                            | Effect                                                      |
|:--------------------------------------|:------------------------------------------------------------|
| `@KeycloakRealms("path/realm.json")`  | Imports realm before test method, removes it after          |
| `@WireMockStub("path/stub.json")`     | Loads WireMock stub before test method, resets all after    |

Stub paths for `@WireMockStub` are resolved relative to the test class package directory. Use a
leading `/` for an absolute classpath path.

### Test categorization

| Annotation         | JUnit 5 tag      | Additional effect                      |
|:-------------------|:-----------------|:---------------------------------------|
| `@UnitTest`        | `unit`           | —                                      |
| `@IntegrationTest` | `integration`    | Activates Spring profile `it`          |

---

## Extensions and Listeners

### Container extensions

Each container extension (`*ContainerExtension`) implements `BeforeAllCallback` / `AfterAllCallback`.
The container instance is a static field, so only one Docker container is started per JVM per
container type, regardless of how many test classes use it.

| Extension                       | Container   | Managed system property               |
|:--------------------------------|:------------|:--------------------------------------|
| `PostgresContainerExtension`    | PostgreSQL  | `spring.datasource.url`               |
| `KafkaContainerExtension`       | Kafka       | `spring.kafka.bootstrap-servers`      |
| `KeycloakContainerExtension`    | Keycloak    | `KC_URL` and `KC_ADMIN_*` properties  |
| `WireMockExtension`             | WireMock    | `wm.url`                              |

### Test execution listeners

| Listener                           | Registration                      | Responsibility                                                     |
|:-----------------------------------|:----------------------------------|:-------------------------------------------------------------------|
| `WireMockExecutionListener`        | auto via `@EnableWireMock`        | Load stubs before test, reset after, fail on unmatched requests    |
| `KeycloakExecutionListener`        | auto via `@EnableKeycloakTlsMode` | Import realms before test method, remove after                     |
| `KafkaTestExecutionListener`       | manual `@TestExecutionListeners`  | Clear Kafka events after each test, stop consumers after class     |
| `LogTestMethodExecutionListener`   | auto via `@LogTestMethod`         | Log test start/finish at INFO under the `TestMethod` category      |

`KafkaTestExecutionListener` is not wired automatically by `@EnableKafka`. Register it explicitly
on integration test classes that need per-test event cleanup:

```java
@EnableKafka
@TestExecutionListeners(
  value = KafkaTestExecutionListener.class,
  mergeMode = MERGE_WITH_DEFAULTS
)
class MyKafkaIntegrationTest { ... }
```

---

## Utility Classes

### `TestUtils`

Central utility class with a pre-configured `ObjectMapper` (non-null serialization, lenient
deserialization) and a thread-safe Kryo pool for deep copies.

| Method                                   | Description                                                           |
|:-----------------------------------------|:----------------------------------------------------------------------|
| `asJsonString(Object)`                   | Serializes an object to a JSON string                                 |
| `parse(String, Class<T>)`                | Deserializes a JSON string to the given type                          |
| `parse(String, TypeReference<T>)`        | Deserializes a JSON string to a generic type                          |
| `parseResponse(MvcResult, Class<T>)`     | Parses a MockMvc response body                                        |
| `readString(String path)`                | Reads a classpath resource as a UTF-8 string                          |
| `readStream(String path)`                | Reads a classpath resource as an `InputStream`                        |
| `copy(T object)`                         | Deep-copies an object using Kryo                                      |
| `verifyNoMoreInteractions(Object)`       | Calls `Mockito.verifyNoMoreInteractions` on all `@Mock`/`@Spy` fields |

### `TestConstants`

| Constant           | Value                       |
|:-------------------|:----------------------------|
| `TENANT_ID`        | `test`                      |
| `OKAPI_AUTH_TOKEN` | `X-Okapi-Token test value`  |

### `TestJwtGenerator`

Generates RS256-signed JWTs using a bundled test RSA private key.

| Method                                               | Description                               |
|:-----------------------------------------------------|:------------------------------------------|
| `generateJwtToken(keycloakUrl, tenant)`              | Valid token, expires in 24 hours          |
| `generateExpiredJwtToken(keycloakUrl, tenant)`       | Token issued and expired 1 hour ago       |

---

## Base Test Class

`BaseBackendIntegrationTest` is an abstract base class for MockMvc-based integration tests. All HTTP
helpers automatically inject the `X-Okapi-Token: X-Okapi-Token test value` header.

```java
@IntegrationTest
@SpringBootTest
@AutoConfigureMockMvc
class MyIntegrationTest extends BaseBackendIntegrationTest {

  @Test
  void getResource_positive() throws Exception {
    doGet("/api/resources/123")
      .andExpect(json("expected-response.json"));
  }
}
```

### HTTP helpers

| Method              | HTTP method  | Success assertion  |
|:--------------------|:-------------|:-------------------|
| `doGet(url)`        | GET          | 200 OK             |
| `doPost(url, body)` | POST         | 201 Created        |
| `doPut(url, body)`  | PUT          | 200 OK             |
| `doDelete(url)`     | DELETE       | 204 No Content     |

`attempt*` variants (e.g., `attemptGet`) perform the request without asserting the status code.

### ResultMatcher helpers

| Method                                        | Asserts                                         |
|:----------------------------------------------|:------------------------------------------------|
| `json("file.json")`                           | Response body matches JSON template file        |
| `notFoundWithMsg("msg")`                      | HTTP 404 with `not_found_error` code            |
| `validationErr(type, msg, field, value)`      | HTTP 400 with `validation_error` code           |
| `argumentNotValidErr(msg, field, value)`      | HTTP 400 with `MethodArgumentNotValidException` |
| `dataIntegrityErr(msg)`                       | HTTP 400 with `service_error` / `PSQLException` |
| `emptyCollection("collectionName")`           | Empty array and `totalRecords: 0`               |

JSON template files are loaded from `classpath:json/<path>` and support `String.format`
placeholders via `readTemplate(path, args...)`.

---

## WireMock Integration

### Stub loading via annotation

```java
@IntegrationTest
@SpringBootTest
@EnableWireMock
@WireMockStub("stubs/downstream-service.json")       // loaded before every test method
class MyServiceTest extends BaseBackendIntegrationTest {

  @Test
  @WireMockStub("stubs/extra-stub.json")              // loaded for this test only
  void callDownstream_positive() throws Exception { ... }
}
```

After each test method `WireMockExecutionListener` resets all stubs and fails the test if any
requests reached WireMock without a matching stub (unmatched-request guard).

### WireMock URL propagation

Create a file `wiremock-url.vars` on the test classpath listing one Spring property name per line.
`WireMockExtension` will set all listed properties to the WireMock URL at startup, allowing the
application under test to discover the WireMock address through its normal configuration.

```
application.clients.downstream-service.url
application.clients.another-service.url
```

### WireMockAdminClient

`WireMockExtension` injects a `WireMockAdminClient` into any field of that type declared in the
test class. Use it for programmatic stub management or request assertions:

```java
@EnableWireMock
class MyTest {

  private WireMockAdminClient wireMockAdminClient;    // injected automatically

  @Test
  void verify_requestCount() {
    var count = wireMockAdminClient.requestCount(
      RequestCriteria.builder()
        .urlPath("/api/resource")
        .method(HttpMethod.GET)
        .build()
    );
    assertThat(count.getCount()).isEqualTo(1);
  }
}
```

---

## Keycloak Integration

Keycloak starts with TLS enabled, a random admin password, and the following pre-configured
features: `scripts:v1`, `token-exchange:v1`, `admin-fine-grained-authz:v1`.

A `folio-backend-admin-client` (secret: `supersecret`) with `admin` and `create-realm` roles is
imported into the master realm on first startup.

```java
@IntegrationTest
@SpringBootTest
@AutoConfigureMockMvc
@EnableKeycloakTlsMode
@EnableKeycloakSecurity
@KeycloakRealms("json/keycloak/test-realm.json")     // imported before every test method
class SecurityIntegrationTest extends BaseBackendIntegrationTest {

  @Test
  @KeycloakRealms("json/keycloak/extra-realm.json")  // imported for this test only
  void authenticate_positive() throws Exception { ... }
}
```

The Keycloak admin client is accessible statically for advanced scenarios:

```java
Keycloak adminClient = KeycloakContainerExtension.getKeycloakAdminClient();
```

---

## Kafka Integration

```java
@IntegrationTest
@SpringBootTest
@EnableKafka
@TestExecutionListeners(
  value = KafkaTestExecutionListener.class,
  mergeMode = MERGE_WITH_DEFAULTS
)
class MyKafkaTest {

  @Test
  void publishEvent_positive() {
    FakeKafkaConsumer.registerTopic("my-topic", MyEvent.class);

    // ... trigger the code that publishes to Kafka ...

    var events = FakeKafkaConsumer.getEvents("my-topic", MyEvent.class);
    assertThat(events).hasSize(1);
    assertThat(events.get(0).value().getId()).isEqualTo("expected-id");
  }
}
```

`KafkaTestExecutionListener` clears all accumulated events after each test method and stops all
consumer containers after the test class completes. The Kafka container is started with
`KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`; topics must be created explicitly before consuming.
