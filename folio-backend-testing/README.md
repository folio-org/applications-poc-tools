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
### Readiness check

The folio-keycloak image runs `configure-realms.sh` as a background process on startup (see
`folio/start.sh` in [folio-org/folio-keycloak](https://github.com/folio-org/folio-keycloak)). That
script calls `folio/setup-admin-client.sh`, which creates a `folio-backend-admin-client` with
`admin` and `create-realm` roles in the master realm. This happens *after* Keycloak's HTTP endpoint
is already healthy, so the standard Testcontainers wait strategy is not sufficient.

`KeycloakContainerExtension` performs an additional wait after `start()` by polling the container
logs until the success message from `setup-admin-client.sh` appears. Tests start only after this
completes, ensuring the admin client is present. The wait is bounded by
`TESTCONTAINERS_KEYCLOAK_READINESS_TIMEOUT` (default 60 s).

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
