# applications-poc-tools

Multi-module Maven library (Spring Boot, Java 21) shared by the FOLIO Eureka managers, the Keycloak-backed modules, and folio-module-sidecar. Downstream repositories consume it as `org.folio:<artifact>:${applications-poc-tools.version}`.

## Commands

```bash
mvn clean install -DskipTests  # build all modules and install the SNAPSHOT into ~/.m2 for downstream repositories
mvn test                       # unit tests (@UnitTest, tag "unit")
mvn verify                     # unit and integration tests (@IntegrationTest, tag "integration"); needs Docker
mvn test -pl folio-security    # one module
mvn test -pl folio-auth-openid -Dtest=JsonWebTokenParserTest  # one unit test class
mvn verify -pl folio-security -Dit.test=KeycloakImportServiceIT -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false  # one IT, unit tests skipped
mvn checkstyle:check           # style only
```

- With `-pl` alone, sibling modules come from `~/.m2` or FOLIO Nexus, not from your working tree. After changing a module that others depend on, run the install command first or add `-am`. With `-am`, a test filter fails in modules that have no matching test, so also pass `-Dsurefire.failIfNoSpecifiedTests=false` (and `-Dfailsafe.failIfNoSpecifiedTests=false` with `-Dit.test`).
- `folio-secret-store` and `folio-integration-kafka` are aggregator poms. Select the real artifacts: `-pl :folio-secret-store-common`, `:folio-secret-store-starter`, `:folio-kafka-common`, `:folio-kafka-producer`, `:folio-kafka-consumer`.
- Integration tests start Testcontainers through the `folio-backend-testing` extensions (`@EnableKafka`, `@EnablePostgres`, `@EnableKeycloakTlsMode`, `@EnableWireMock`). Override an image with `TESTCONTAINERS_<NAME>_IMAGE` (see `DockerImageRegistry`).
- Checkstyle runs at `process-classes`, so every build above fails on a violation, test sources included. The rules come from the `folio-java-checkstyle` artifact; `checkstyle/checkstyle-checker.properties` only sets the method length limit to 23 lines, and `checkstyle/checkstyle-suppressions.xml` holds the suppressions.

## Conventions

- Every module is a public API for the downstream repositories listed in the "Verify Dependent Modules" GitHub Actions workflow. Run that workflow on the branch before merging an API or dependency change (`docs/workflows/verify-dependent-modules.md`). Record the change in `NEWS.md` under the in-progress version with the Jira key, and start the entry with `**Breaking:**` when downstream code must change.
- `folio-auth-openid` and `folio-secret-store-common` are used by folio-module-sidecar, which is a Quarkus application. Keep Spring types out of them; the Spring wiring for the secret store lives in `folio-secret-store-starter`.
- Consumer applications component-scan only their own packages (for example `org.folio.tm`), so a new `@Component` here is never loaded. Register library beans in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` or in the `@Import` list of `@EnableMgrSecurity`, `@EnableKafkaProducer`, or `@EnableKafkaConsumer`. Gate beans that need configuration behind a property, because every consumer of the annotation gets them.
- The gateway library is selected by `application.apigw.type`: `kong` (also when the property is missing) or `apisix`. `ApiGatewayTypeValidationAutoConfiguration` fails startup on any other value when `application.apigw.enabled` is true.
- JSON is Jackson 3 (`tools.jackson.*`), but annotations stay on `com.fasterxml.jackson.annotation.*`.
- HTTP clients are `@HttpExchange` interfaces built with `HttpClientTlsUtils.buildHttpServiceClient` (`folio-tls-utils`), so TLS settings apply. Resteasy is used only for the Keycloak admin client (`ClientBuildUtils`).

## Gotchas

- `@EnableMgrSecurity` fails open. The Keycloak `AuthorizationService` exists only when both `application.security.enabled` and `application.keycloak.enabled` are true; otherwise `SecurityConfiguration` registers a permit-all filter chain. Even with Keycloak on, `SecurityConfiguration.customize` skips security for actuator endpoints and for every `GET` request except `<path-prefix>entitlements/.*/applications`.
- `FolioEnvironment` has two sources. The bean field reads `application.environment` (required, so `@EnableKafkaProducer` fails startup without it), but `KafkaUtils` builds topic names from the static `getFolioEnvName()`, which reads the `ENV` variable or the `env` system property and defaults to `folio`. Consumers set `application.environment: ${ENV:folio}` so both agree.
- The secret-store key prefix is `application.secret-store.environment` (no default; startup fails when it is empty). It is separate from the Kafka prefix: consumers bind it to `SECURE_STORE_ENV` and fall back to `ENV` (`${SECURE_STORE_ENV:${ENV:folio}}`).
- Without `application.secret-store.type`, `SecureStoreAutoconfiguration` creates `EphemeralStore`: an in-memory map seeded from `application.secret-store.ephemeral.content`. A missing setting does not fail startup, and secrets written at runtime are lost on restart.
- `@EnableKeycloakDataImport` sets `application.secure-store.environment` (typo), which nothing reads. Tests that use it must set `application.secret-store.environment` themselves, as `KeycloakImportServiceIT` does.
- `DeprecatedKongPropertiesPostProcessor` only logs warnings for legacy `application.kong.*` properties and the legacy variables it lists (`KONG_ADMIN_URL`, `REGISTER_MODULE_IN_KONG`, ...). It does not map them to `application.apigw.*`; a legacy name works only where the consumer's `application.yaml` chains it.
