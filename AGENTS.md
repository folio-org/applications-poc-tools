# applications-poc-tools

Multi-module Maven library of shared classes for FOLIO backend development and testing. Java, Spring Boot, Log4j2; modules for Keycloak security, Kafka, secret stores, Kong and APISIX gateways, and Testcontainers-based testing.

## Build & Test

```bash
mvn clean install              # full build
mvn clean install -DskipTests  # skip tests
mvn test                       # unit tests (groups=unit, surefire)
mvn verify                     # unit and integration tests (failsafe runs groups=integration); needs Docker
mvn test -pl folio-security    # single module
mvn test -pl folio-auth-openid -Dtest=JsonWebTokenParserTest  # single class
mvn checkstyle:check           # runs at process-classes; violations fail build
```

After API/dependency changes, run the "Verify Dependent Modules" GitHub Actions workflow. Dependents: mgr-applications, mgr-tenants, mgr-tenant-entitlements, mod-roles-keycloak, mod-login-keycloak, mod-users-keycloak, mod-scheduler, mod-consortia-keycloak, folio-module-sidecar.

## Modules

Each module is a top-level directory with its own `README.md`; `folio-integration-kafka` and `folio-secret-store` are aggregators of the sub-modules in brackets.

- `folio-backend-common` — shared descriptor model, errors, validation, CQL and pagination helpers.
- `folio-backend-testing` — JUnit extensions and Testcontainers wrappers (Postgres, Kafka, Keycloak, WireMock) for consumer modules' tests.
- `folio-auth-openid` — JWT parsing and validation against Keycloak JWKS; no Spring dependency.
- `folio-security` — Spring Security filter chain with Keycloak authorization.
- `folio-integration-kafka` [`folio-kafka-common`, `folio-kafka-producer`, `folio-kafka-consumer`] — topic management and tenant-aware producers and consumers.
- `folio-secret-store` [`folio-secret-store-common`, `folio-secret-store-starter`] — secret store backends (AWS SSM, Vault, FSSP, ephemeral).
- `folio-integration-kong`, `folio-integration-apisix` — API gateway route management and module self-registration.
- `folio-tls-utils` — truststore-backed TLS for HTTP clients.
- `folio-permission-utils` — FOLIO permission name parser; no Spring dependency.

## Conventions

- **Feature activation**: annotations (`@EnableMgrSecurity`, `@EnableKafkaProducer`, `@EnableKafkaConsumer`) + properties (`application.apigw.enabled`, `application.apigw.register-module`).
- **Tests**: tag `@UnitTest`/`@IntegrationTest`; surefire runs `groups=unit`, failsafe `groups=integration`.
- **Checkstyle**: rules come from the `folio-java-checkstyle` artifact; `checkstyle/` holds only the properties and suppressions.
- **Jackson 3**: runtime in `tools.jackson.*`; annotations stay on `com.fasterxml.jackson.annotation.*`. ObjectMapper uses `.rebuild()...build()`. Jackson 2 also on classpath (JWT/Resteasy) — no conflict.
- **HTTP clients**: Spring HTTP Service Clients (`@HttpExchange`) over RestClient; Resteasy only for Keycloak admin client.
- **Null-safety**: JSpecify (`@Nullable`/`@NonNull`); Spring `org.springframework.lang.*` is deprecated.
- **Secret store env var**: use `SECURE_STORE_ENV` (not `ENV`) → `application.secret-store.environment=${SECURE_STORE_ENV:folio}`.
- **Security plugin**: `KeycloakSecurityConfiguration` imported by `SecurityConfiguration`; activated by `application.yaml` (no code change). With no `AuthorizationService` bean, a permit-all chain is registered.
- **Lombok**: configured via root `lombok.config`; processor declared in maven-compiler-plugin.
