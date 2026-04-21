# folio-backend-testing

Testing infrastructure and utilities for FOLIO backend integration tests. Provides JUnit 5 extensions, Testcontainers wrappers, WireMock support, base test classes, and JWT generation.

## Test Annotations

### Test Categorization

- `@UnitTest` -- tags a test as a unit test (runs with Surefire)
- `@IntegrationTest` -- tags a test as an integration test with `@ActiveProfiles("it")` (runs with Failsafe)

### Container Extensions

| Annotation | Container | System Property Set |
|------------|-----------|---------------------|
| `@EnableKafka` | Kafka (Testcontainers) | `spring.kafka.bootstrap-servers` |
| `@EnablePostgres` | PostgreSQL (Testcontainers) | Database connection properties |
| `@EnableWireMock` | WireMock | `wm.url` + properties listed in `wiremock-url.vars` |
| `@EnableKeycloakSecurity` | Keycloak (Testcontainers) | Keycloak connection properties |

### WireMock Stubs

Load WireMock stubs from JSON files using `@WireMockStub`:

```java
@EnableWireMock
@WireMockStub(scripts = "/wiremock/stubs/my-stub.json")
class MyIntegrationTest { }
```

To map additional system properties to the WireMock URL, create a `wiremock-url.vars` file in `src/test/resources` with one property name per line (e.g., `okapi.url`).

### Keycloak

- `@EnableKeycloakDataImport` -- activates Keycloak realm/client data import
- `@EnableKeycloakTlsMode` -- enables TLS mode for Keycloak container
- `@KeycloakRealms` -- specifies Keycloak realm JSON files to import

## Utilities

- `TestUtils` -- JSON serialization helpers (`asJsonString`, `parse`, `readString`)
- `TestConstants` -- shared test constants (e.g., `OKAPI_AUTH_TOKEN`)
- `TestJwtGenerator` -- generates signed JWT tokens for security integration tests
- `FakeKafkaConsumer` -- in-test Kafka consumer for verifying produced messages
- `BaseBackendIntegrationTest` -- abstract base class with `MockMvc` wired and HTTP helper methods

## Usage

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-backend-testing</artifactId>
  <version>${applications-poc-tools.version}</version>
  <scope>test</scope>
</dependency>
```

### Transitive Test Dependencies

This module brings in:

- `spring-boot-starter-test`
- `spring-kafka`
- `org.testcontainers:testcontainers`, `kafka`, `postgresql`
- `mockito-bom` 5.x
- `instancio-junit`
