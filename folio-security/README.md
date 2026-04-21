# folio-security

Spring Security integration layer supporting both Okapi (legacy token-based) and Keycloak (JWT/OIDC) authorization flows for FOLIO backend services.

## Setup

Add `@EnableMgrSecurity` to your configuration class:

```java
@EnableMgrSecurity
@SpringBootApplication
public class MyApplication { }
```

This imports the base `SecurityConfiguration` which sets up the `SecurityFilterChain` with the authorization and exception-handling filters.

## Authorization Flows

### Okapi (Legacy)

Token-based authorization via `mod-authtoken`. The `OkapiAuthorizationService` validates tokens against the Okapi authtoken endpoint and matches incoming requests against the module's `RoutingEntry` definitions.

Configuration:

```yaml
application:
  okapi:
    enabled: true
    url: http://localhost:9130
```

### Keycloak (JWT/OIDC)

JWT-based authorization using Keycloak. The `KeycloakAuthorizationService` validates JWT tokens against Keycloak JWKS public keys and supports automatic import of module permissions/roles into Keycloak realms.

Configuration:

```yaml
application:
  keycloak:
    enabled: true
    url: http://localhost:8080
    admin:
      client-id: admin-cli
    client:
      client-id: my-module
    tls:
      enabled: false
```

## Key Classes

### Core

- `AuthorizationFilter` -- servlet filter that intercepts requests and invokes authorization
- `ExceptionHandlerFilter` -- translates security exceptions into FOLIO error responses
- `AuthorizationService` -- interface for `authorize(request, token)`
- `AbstractAuthorizationService` -- base implementation with routing entry matching
- `InternalModuleDescriptorProvider` -- provides the module's own `ModuleDescriptor` for route matching
- `AuthUserPrincipal` -- `UserDetails`-based principal holding tenant, userId, and token

### Keycloak Integration

- `KeycloakAuthorizationService` -- JWT validation via Keycloak
- `KeycloakTokenValidator` -- validates JWT tokens against JWKS public keys
- `KeycloakPublicKeyProvider` -- fetches and caches JWKS keys
- `KeycloakImportService` -- imports module permissions/roles into Keycloak realms
- `SecureStoreKeyProvider` -- retrieves client secrets from the configured `SecureStore`

## Dependencies

This module depends on `folio-backend-common`, `folio-tls-utils`, `folio-auth-openid`, and `folio-secret-store-common`.

## Usage

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-security</artifactId>
  <version>${applications-poc-tools.version}</version>
</dependency>
```
