# AGENTS.md - Coding Agent Guidelines for applications-poc-tools

## Project Overview

This is **applications-poc-tools**, a multi-module Maven library providing general-purpose classes for FOLIO backend development and testing. It's built on Spring Boot 4.0.2 with Java 21 and uses Log4j2 for logging.

## Build Commands

### Build entire project
```bash
mvn clean install
```

### Build specific module
```bash
cd folio-backend-common
mvn clean install
```

### Run unit tests only
```bash
mvn test
```

### Run integration tests
```bash
mvn verify
```

### Run tests for a specific module
```bash
mvn test -pl folio-security
```

### Run checkstyle
```bash
mvn checkstyle:check
```

### Skip tests during build
```bash
mvn clean install -DskipTests
```

### Run a single test class
```bash
mvn test -Dtest=JsonWebTokenParserTest
```

### Verify dependent modules (after API changes)
Trigger the "Verify Dependent Modules" GitHub Actions workflow manually via GitHub UI or CLI.

## Module Architecture

### Core Modules and Dependencies

```
folio-backend-common (foundation layer)
  ├── folio-backend-testing (test infrastructure)
  ├── folio-tls-utils (SSL/TLS utilities)
  ├── folio-security (authentication/authorization)
  │   ├── folio-auth-openid (JWT parsing)
  │   ├── folio-secret-store (secret management)
  │   └── folio-integration-kafka (event streaming)
  ├── folio-integration-kong (API Gateway)
  └── folio-permission-utils (permission parsing)
```

### folio-backend-common
Foundation module providing:
- Domain models: `ApplicationDescriptor`, `ModuleDescriptor`, `Capability`, `Permission`
- Utilities: `CqlQuery`, `SemverUtils`, `OkapiHeaders`, `UuidUtils`
- Services: Transaction helpers, REST client support
- Location: Root utilities used by all other modules

### folio-security
Pluggable security framework with two authentication backends:
- **Keycloak** (modern): OAuth2/OpenID Connect with JWT validation
- **Okapi** (legacy): Token-based authentication for legacy FOLIO systems
- Key classes: `AuthorizationFilter`, `KeycloakAuthorizationService`, `OkapiAuthorizationService`
- HTTP clients: Uses Spring HTTP Service Clients (Spring Framework 6.1+) for remote API calls
- Activation: Use `@EnableMgrSecurity` annotation to enable security configuration
- Backend selection: Controlled via application properties at runtime

### folio-auth-openid
JWT token parsing and validation:
- `JsonWebTokenParser`: Validates JWT structure and issuer claims
- `OpenidJwtParserProvider`: Provides cached JWT parsers with automatic cache invalidation on key rotation
- Uses Jackson 3 for JSON processing
- No Spring dependencies (minimal library)

### folio-secret-store
Multi-backend secret management with factory pattern:
- **Implementations**: VaultStore (HashiCorp Vault), AwsStore (AWS SSM), FsspStore (File System), EphemeralStore (in-memory)
- **Common module**: Pure Java, no Spring dependencies
- **Starter module**: Spring Boot auto-configuration bridge
- `SecureStoreFactory`: Selects implementation based on type string

### folio-backend-testing
Test infrastructure with TestContainers:
- Base class: `BaseBackendIntegrationTest`
- Annotations: `@EnablePostgres`, `@EnableKafka`, `@EnableKeycloakSecurity`, `@EnableWireMock`
- Test markers: `@IntegrationTest` (runs via maven-failsafe-plugin), `@UnitTest` (runs via maven-surefire-plugin)
- Utilities: `TestJwtGenerator`, `FakeKafkaConsumer`
- Uses Jackson 3 for JSON processing in tests

### folio-integration-kafka
Spring Kafka abstraction:
- `@EnableKafka`: Activates topic configuration
- `KafkaAdminService`: Topic management operations
- `FolioKafkaProperties`: Externalized Kafka configuration
- Uses Spring Boot 4 Kafka starter

### folio-integration-kong
Kong API Gateway integration:
- `KongGatewayService`: Gateway operations
- `KongModuleRegistrar`: Application registration lifecycle
- `KongRouteTenantService`: Multi-tenant route management
- Route DSL: Type-safe builders (`StringExpressionBuilder`, `IntExpressionBuilder`, `IpAddressOperator`)
- HTTP clients: Uses Spring HTTP Service Clients for Kong Admin API communication

### folio-tls-utils
SSL/TLS configuration for HTTP clients:
- `HttpClientTlsUtils`: Build SSL-enabled HTTP Service Clients with custom keystores
- `ClientBuildUtils`: Build SSL-enabled Resteasy clients
- Supports PKCS12, JKS keystores
- Configurable hostname verification (supports debug mode)
- Provides `Utils.buildSslContext()` for creating SSLContext from TLS properties

### folio-permission-utils
Permission string parsing and classification:
- `extractPermissionData()`: Parses permission names into structured data
- Types: DATA, SETTINGS, PROCEDURAL
- Actions: VIEW, CREATE, EDIT, DELETE, MANAGE, EXECUTE
- Pure Java utility (no Spring dependency)

## Common Patterns

### Annotation-Based Activation
Modules use annotations to activate features:
- `@EnableMgrSecurity` → SecurityConfiguration
- `@EnableKafka` → KafkaTopicConfiguration
- `@EnableKeycloakSecurity`, `@EnablePostgres` → Test extensions

### Property-Driven Configuration
Feature toggles via application properties:
- `application.kong.enabled` → Gates Kong initialization
- `application.kong.register-module` → Gates module registration
- Security backend selection (Keycloak vs Okapi) → Runtime configuration

### Testing Strategy
- **Unit tests**: Annotated with `@UnitTest`, run via maven-surefire-plugin (groups=unit)
- **Integration tests**: Annotated with `@IntegrationTest`, run via maven-failsafe-plugin (groups=integration)
- TestContainers: Used for Postgres, Kafka, Keycloak, WireMock
- Mockito v5.20.0 for mocking

### Checkstyle
- Uses FOLIO Java checkstyle rules (`folio-java-checkstyle:1.2.0`)
- Configuration: `checkstyle/checkstyle.xml` (from folio-java-checkstyle)
- Suppressions: `checkstyle/checkstyle-suppressions.xml`
- Runs during `process-classes` phase
- Violations cause build failure

## Technology Stack

### Spring Boot 4.0.2
- Upgraded from Spring Boot 3.5.7
- Uses Spring Framework 7.0
- Jackson 3.x for JSON processing (`tools.jackson.*` package namespace)
- Spring HTTP Service Clients for declarative HTTP APIs (replaces OpenFeign)

### Jackson 3.x
- Package namespace: `tools.jackson.core.*` and `tools.jackson.databind.*`
- Annotations still use Jackson 2.x namespace: `com.fasterxml.jackson.annotation.*` (by design)
- ObjectMapper uses builder pattern: `new ObjectMapper().rebuild()...build()`
- Jackson 2.x also on classpath (from JWT/Resteasy libraries) - no conflicts due to different packages

### HTTP Clients
- **Spring HTTP Service Clients**: Modern declarative HTTP client using `@HttpExchange` annotations
- **RestClient**: Underlying HTTP client for Spring HTTP Service Clients
- **Resteasy**: Used by Keycloak admin client
- All support SSL/TLS configuration via `HttpClientTlsUtils` and `ClientBuildUtils`

### Null-Safety
- Uses JSpecify annotations (`org.jspecify.annotations.*`)
- `@Nullable`: Marks types that can be null
- `@NonNull`: Marks types that cannot be null (explicit annotation)
- Spring Framework 7 deprecated `org.springframework.lang.*` null-safety annotations

## Important Notes

### Security Architecture
The security module implements a plugin pattern:
- Both `KeycloakSecurityConfiguration` and `OkapiSecurityConfiguration` are imported
- Spring creates beans only for the active backend based on properties
- Switch backends by changing application.yaml (no code changes needed)

### Secret Store Environment Variable
- Use `SECURE_STORE_ENV` environment variable (NOT `ENV`)
- Configuration: `application.secret-store.environment=${SECURE_STORE_ENV:folio}`
- See NEWS.md for migration details

### Test Categories
Tests MUST be annotated with markers:
- `@Tag("unit")` for unit tests
- `@Tag("integration")` for integration tests
- Maven surefire runs `groups=unit`, failsafe runs `groups=integration`

### Lombok Configuration
- `lombok.config` at root configures annotation processing
- Lombok annotation processor declared in maven-compiler-plugin

### Dependent Modules
After API or dependency changes, verify dependent modules build successfully:
- Dependent services: mgr-applications, mgr-tenants, mgr-tenant-entitlements, mod-roles-keycloak, mod-login-keycloak, mod-users-keycloak, mod-scheduler, mod-consortia-keycloak, folio-module-sidecar
- Trigger "Verify Dependent Modules" workflow via GitHub Actions

## Repository Information

- GitHub: https://github.com/folio-org/applications-poc-tools
- Main branch: `master`
- Maven repositories: FOLIO Nexus (https://repository.folio.org/repository/maven-folio)
- License: Apache License 2.0
