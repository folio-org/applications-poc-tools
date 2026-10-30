# applications-poc-tools

Multi-module Maven library of shared classes for FOLIO backend development and testing. Spring Boot, Java 21, Log4j2.

## Build & Test

```bash
mvn clean install              # full build
mvn clean install -DskipTests  # skip tests
mvn test                       # unit tests (groups=unit, surefire)
mvn verify                     # unit + integration tests (groups=integration, failsafe)
mvn test -pl folio-security    # single module
mvn test -pl folio-auth-openid -Dtest=JsonWebTokenParserTest  # single class (needs -pl: the class is not in the root build)
mvn verify -pl folio-security -Dit.test=KeycloakImportServiceIT -Dtest=SkipSurefire -Dsurefire.failIfNoSpecifiedTests=false  # single IT, skips unit tests
mvn checkstyle:check           # runs at process-classes; violations fail build
```

After API/dependency changes, run the "Verify Dependent Modules" GitHub Actions workflow. Dependents: mgr-applications, mgr-tenants, mgr-tenant-entitlements, mod-roles-keycloak, mod-login-keycloak, mod-users-keycloak, mod-scheduler, mod-consortia-keycloak, folio-module-sidecar.

## Modules

`->` lists compile dependencies on sibling modules. Most modules also use `folio-backend-testing` in test scope.

```
folio-backend-common      Foundation: ApplicationDescriptor, ModuleDescriptor, CqlQuery, OkapiHeaders; gateway-agnostic ApiGatewayService, ApiGatewayModuleRegistrar
folio-backend-testing     MockMvc base (BaseBackendIntegrationTest); Testcontainers extensions @EnablePostgres/@EnableKafka/@EnableWireMock/@EnableKeycloakSecurity; TestJwtGenerator
folio-tls-utils           SSL/TLS for HTTP clients (HttpClientTlsUtils); PKCS12/JKS
folio-auth-openid         JWT parsing (JsonWebTokenParser, OpenidJwtParserProvider); pure Java, Jackson 3
folio-secret-store        Secrets: Vault/AWS-SSM/FsspStore/Ephemeral via SecureStoreFactory
folio-security            Keycloak (OAuth2/OIDC) auth; @EnableMgrSecurity, ClientBuildUtils; activated by properties -> common, tls-utils, auth-openid, secret-store
folio-integration-kafka   Spring Kafka abstraction; @EnableKafkaProducer/@EnableKafkaConsumer, KafkaAdminService -> common
folio-integration-kong    Kong Admin API: KongGatewayService, route DSL -> common, tls-utils
folio-integration-apisix  Apache APISIX Admin API: ApisixGatewayService -> common, tls-utils
folio-permission-utils    Permission parsing (DATA/SETTINGS/PROCEDURAL; VIEW/CREATE/EDIT/DELETE/MANAGE/EXECUTE); pure Java
```

## Conventions and pitfalls

- Feature activation: annotations (`@EnableMgrSecurity`, `@EnableKafkaProducer`, `@EnableKafkaConsumer`) + properties (`application.apigw.enabled`, `application.apigw.type` = `kong` (default) or `apisix`, `application.apigw.register-module`).
- Tests: tag `@UnitTest`/`@IntegrationTest`; surefire runs `groups=unit`, failsafe `groups=integration`. Mockito v5.
- Jackson 3: runtime in `tools.jackson.*`; annotations stay on `com.fasterxml.jackson.annotation.*`. ObjectMapper uses `.rebuild()...build()`. Jackson 2 also on classpath (JWT/Resteasy); no conflict.
- HTTP clients: Spring HTTP Service Clients (`@HttpExchange`) over RestClient; Resteasy only for the Keycloak admin client.
- Null-safety: JSpecify (`@Nullable`/`@NonNull`); Spring `org.springframework.lang.*` is deprecated here.
- Secret store env var: consuming modules set `application.secret-store.environment=${SECURE_STORE_ENV:folio}` in `application.yaml`; the property has no default. Do not map it to `ENV`, which sets the Kafka topic prefix (`FolioEnvironment`).
- Security plugin: `KeycloakSecurityConfiguration` is imported by `SecurityConfiguration` and activated by `application.yaml` (no code change). With no `AuthorizationService` bean, a permit-all chain is registered.
- Checkstyle: `folio-java-checkstyle`; config and suppressions under `checkstyle/`.

## Repo

GitHub: https://github.com/folio-org/applications-poc-tools · branch `master` · Nexus: https://repository.folio.org/repository/maven-folio · Apache-2.0. Full env vars in `README.md`.
