# applications-poc-tools

Multi-module Maven library of shared classes for FOLIO backend development and testing. Spring Boot 4.0.2, Java 21, Log4j2.

## Build & Test

```bash
mvn clean install              # full build
mvn clean install -DskipTests  # skip tests
mvn test                       # unit tests (groups=unit, surefire)
mvn verify                     # integration tests (groups=integration, failsafe)
mvn test -pl folio-security    # single module
mvn test -Dtest=JsonWebTokenParserTest  # single class
mvn checkstyle:check           # runs at process-classes; violations fail build
```

After API/dependency changes, run the "Verify Dependent Modules" GitHub Actions workflow. Dependents: mgr-applications, mgr-tenants, mgr-tenant-entitlements, mod-roles-keycloak, mod-login-keycloak, mod-users-keycloak, mod-scheduler, mod-consortia-keycloak, folio-module-sidecar.

## Modules

```
folio-backend-common (foundation: ApplicationDescriptor, ModuleDescriptor, CqlQuery, OkapiHeaders)
  ├── folio-backend-testing   TestContainers base (BaseBackendIntegrationTest); @EnablePostgres/@EnableKafka/@EnableKeycloakSecurity/@EnableWireMock; TestJwtGenerator
  ├── folio-tls-utils          SSL/TLS for HTTP clients (HttpClientTlsUtils, ClientBuildUtils); PKCS12/JKS
  ├── folio-security           Pluggable auth: Keycloak (OAuth2/OIDC) or Okapi (legacy); @EnableMgrSecurity; backend chosen by properties
  │   ├── folio-auth-openid    JWT parsing (JsonWebTokenParser, OpenidJwtParserProvider); pure Java, Jackson 3
  │   ├── folio-secret-store   Secrets: Vault/AWS-SSM/FsspStore/Ephemeral via SecureStoreFactory
  │   └── folio-integration-kafka  Spring Kafka abstraction; @EnableKafka, KafkaAdminService
  ├── folio-integration-kong   Kong Admin API: KongGatewayService, KongModuleRegistrar, route DSL
  └── folio-permission-utils   Permission parsing (DATA/SETTINGS/PROCEDURAL; VIEW/CREATE/EDIT/DELETE/MANAGE/EXECUTE); pure Java
```

## Conventions

- **Feature activation**: annotations (`@EnableMgrSecurity`, `@EnableKafka`) + properties (`application.kong.enabled`, `application.kong.register-module`).
- **Tests**: tag `@UnitTest`/`@IntegrationTest`; surefire runs `groups=unit`, failsafe `groups=integration`. Mockito v5.
- **Checkstyle**: `folio-java-checkstyle:1.2.0`; config/suppressions under `checkstyle/`.
- **Jackson 3**: runtime in `tools.jackson.*`; annotations stay on `com.fasterxml.jackson.annotation.*`. ObjectMapper uses `.rebuild()...build()`. Jackson 2 also on classpath (JWT/Resteasy) — no conflict.
- **HTTP clients**: Spring HTTP Service Clients (`@HttpExchange`) over RestClient; Resteasy only for Keycloak admin client.
- **Null-safety**: JSpecify (`@Nullable`/`@NonNull`); Spring `org.springframework.lang.*` is deprecated.
- **Secret store env var**: use `SECURE_STORE_ENV` (not `ENV`) → `application.secret-store.environment=${SECURE_STORE_ENV:folio}`.
- **Security plugin**: both Keycloak and Okapi configs imported; active backend selected by `application.yaml` (no code change).
- **Lombok**: configured via root `lombok.config`; processor declared in maven-compiler-plugin.

## Repo

GitHub: https://github.com/folio-org/applications-poc-tools · branch `master` · Nexus: https://repository.folio.org/repository/maven-folio · Apache-2.0. Full env vars in `README.md`.
