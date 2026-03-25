# folio-security

Spring Security integration library for FOLIO backend modules. Provides a pluggable authorization
filter chain supporting two security backends — Keycloak (JWT + UMA) and Okapi (`mod-authtoken`) —
with automatic route matching against the module's own descriptor, Keycloak data import at startup,
and a standardized JSON error response format.

## Table of Contents

- [Activation](#activation)
- [Configuration](#configuration)
- [Security Backends](#security-backends)
  - [Keycloak](#keycloak)
  - [Okapi](#okapi)
- [Keycloak Data Import](#keycloak-data-import)
- [Module Descriptor](#module-descriptor)
- [Filter Chain](#filter-chain)
- [Extension Points](#extension-points)

---

## Activation

Add `@EnableMgrSecurity` to a Spring `@Configuration` class to activate the security framework:

```java
@Configuration
@EnableMgrSecurity
public class ApplicationConfiguration { }
```

This imports `SecurityConfiguration`, which registers the Spring Security filter chain
(stateless, CSRF-disabled) and wires either the Keycloak or Okapi authorization service
depending on the active configuration properties.

When no `AuthorizationService` bean is present, a passthrough filter chain is registered that
permits all requests (useful for local development).

---

## Configuration

### Shared properties

| Property                         | Type      | Description                                                                                         |
|:---------------------------------|:----------|:----------------------------------------------------------------------------------------------------|
| `application.security.enabled`   | `boolean` | Master switch to activate the filter chain                                                          |
| `application.router.path-prefix` | `String`  | Optional path prefix stripped before route matching against the module descriptor (e.g. `/mgr-foo`) |

### Keycloak properties

| Property                                                                    | Type      | Description                                              |
|:----------------------------------------------------------------------------|:----------|:---------------------------------------------------------|
| `application.keycloak.enabled`                                              | `boolean` | Activate Keycloak-mode security                          |
| `application.keycloak.url`                                                  | `String`  | Public-facing Keycloak base URL                          |
| `application.keycloak.jwks-base-url`                                        | `String`  | Override internal JWKS URL (e.g. for in-cluster routing) |
| `application.keycloak.impersonation-client`                                 | `String`  | Impersonation client name                                |
| `application.keycloak.admin.client-id`                                      | `String`  | Admin client ID                                          |
| `application.keycloak.admin.username`                                       | `String`  | Admin username                                           |
| `application.keycloak.admin.password`                                       | `String`  | Admin password                                           |
| `application.keycloak.admin.grant-type`                                     | `String`  | Admin grant type                                         |
| `application.keycloak.client.client-id`                                     | `String`  | Backend service client ID                                |
| `application.keycloak.tls.enabled`                                          | `boolean` | Enable TLS for Keycloak HTTP client                      |
| `application.keycloak.tls.trust-store-path`                                 | `String`  | Truststore file path                                     |
| `application.keycloak.tls.trust-store-password`                             | `String`  | Truststore password                                      |
| `application.keycloak.tls.trust-store-type`                                 | `String`  | Truststore type (e.g. `JKS`, `PKCS12`)                   |
| `application.keycloak.jwt-cache-configuration.validate-uri`                 | `boolean` | Validate token issuer against `keycloak.url`             |
| `application.keycloak.jwt-cache-configuration.jwks-refresh-interval`        | `int`     | JWKS refresh interval in seconds (default: `60`)         |
| `application.keycloak.jwt-cache-configuration.forced-jwks-refresh-interval` | `int`     | Forced JWKS refresh interval in seconds (default: `60`)  |

### Okapi properties

| Property                              | Type      | Description                  |
|:--------------------------------------|:----------|:-----------------------------|
| `application.okapi.enabled`           | `boolean` | Activate Okapi-mode security |
| `application.okapi.url`               | `String`  | Base Okapi URL               |
| `application.okapi.mod-authtoken-url` | `String`  | URL of `mod-authtoken`       |

---

## Security Backends

### Keycloak

Activated when `application.security.enabled=true` and `application.keycloak.enabled=true`.

**Authorization flow:**

1. `AuthorizationFilter` extracts the bearer token from `Authorization` or `X-Okapi-Token`.
2. The token's `iss` claim is extracted and used to select the per-realm JWT parser.
3. SmallRye JWT validates the token signature against Keycloak's JWKS endpoint and checks expiry.
4. The matching `RoutingEntry` is found in the module descriptor for the request path and method.
5. If the route has no `permissionsRequired`, the tenant claim is validated and the request is allowed.
6. If the route has `permissionsRequired`, a UMA permission evaluation request is sent to Keycloak's
   token endpoint (`POST /realms/{realm}/protocol/openid-connect/token`) with the route's resource
   and HTTP method as the permission string.
7. A `403` response from Keycloak throws `ForbiddenException`; `401` throws `NotAuthorizedException`.

**Public key rotation:** When SmallRye JWT cannot find the signing key (key rotation), the per-realm
parser cache is automatically evicted and the JWKS re-fetched on the next request.

### Okapi

Activated when `application.security.enabled=true`, `application.okapi.enabled=true`, and
`application.keycloak.enabled=false`.

**Authorization flow:**

1. `AuthorizationFilter` extracts the bearer token.
2. The matching `RoutingEntry` is resolved from the module descriptor.
3. `permissionsRequired`, `permissionsDesired`, and `modulePermissions` are extracted from the entry.
4. A call is delegated to `mod-authtoken` with those permission headers. Okapi validates the token
   and checks the user's permissions.
5. On success, the JWT payload is Base64-decoded (without re-verifying the signature) to extract the
   `user_id` and `tenant` claims for the `AuthUserPrincipal`.

---

## Keycloak Data Import

Activated when `application.keycloak.enabled=true` and `application.keycloak.import.enabled=true`
(Spring Boot auto-configuration).

On `ApplicationReadyEvent`, `KeycloakImportService` imports the module's own descriptor into Keycloak:

1. Reads `classpath:descriptors/ModuleDescriptor.json` and computes its SHA hash.
2. Checks whether the module's Keycloak client already exists.
   - If absent: creates the client, realm roles, role policies, and scope permissions.
   - If present and hash matches: skips import but re-assigns roles to the admin service account.
   - If present and hash differs: removes the outdated client and recreates everything.
3. Assigns all created realm roles to the service account of `folio-backend-admin-client`.

The admin client secret is read from the pluggable `SecureStore` using the key pattern
`{env}_master_{adminClientId}`. A warning is logged when the secret is not found.

| Property                              | Type      | Description                         |
|:--------------------------------------|:----------|:------------------------------------|
| `application.keycloak.import.enabled` | `boolean` | Enable descriptor import at startup |

---

## Module Descriptor

Every consuming module **must** provide `classpath:descriptors/ModuleDescriptor.json`. This file
drives both route matching (authorization) and Keycloak data import.

```
src/main/resources/
  descriptors/
    ModuleDescriptor.json
```

`InternalModuleDescriptorProvider` loads and caches this file at startup. It is declared as
`@ConditionalOnMissingBean` in all configurations, allowing consumers to override it with a custom
provider.

---

## Filter Chain

The Spring Security filter chain registers two custom filters before the built-in
`AuthorizationFilter`:

```
Request
  └─ ExceptionHandlerFilter      ← translates security exceptions → JSON ErrorResponse
       └─ AuthorizationFilter    ← token extraction, RoutingEntry lookup, backend delegation
            └─ Spring chain
```

### Bypassed paths (no auth required)

- All Spring Boot actuator endpoints (`/actuator/**`)
- All `GET` requests except `GET /<router-prefix>/entitlements/*/applications`

### Exception mapping

| Exception                       | HTTP Status | Error Code              |
|:--------------------------------|:------------|:------------------------|
| `NotAuthorizedException`        | `401`       | `AUTH_ERROR`            |
| `ForbiddenException`            | `403`       | `AUTH_ERROR`            |
| `TokenMismatchException`        | `400`       | `FOUND_ERROR`           |
| `RoutingEntryMatchingException` | `404`       | `ROUTE_NOT_FOUND_ERROR` |
| `IllegalArgumentException`      | `400`       | `VALIDATION_ERROR`      |
| Any other exception             | `500`       | `UNKNOWN_ERROR`         |

### `AuthUserPrincipal`

After successful authorization, the authenticated principal placed in `SecurityContextHolder` contains:

| Field        | Source                                                             |
|:-------------|:-------------------------------------------------------------------|
| `userId`     | JWT `user_id` claim                                                |
| `authUserId` | JWT `sub` claim                                                    |
| `tenant`     | Last segment of JWT `iss` (Keycloak) or JWT `tenant` claim (Okapi) |

---

## Extension Points

| Bean                               | Condition                   | Description                                                                  |
|:-----------------------------------|:----------------------------|:-----------------------------------------------------------------------------|
| `AuthorizationService`             | `@ConditionalOnBean`        | Replace with a custom implementation to plug in a different security backend |
| `InternalModuleDescriptorProvider` | `@ConditionalOnMissingBean` | Override to load the module descriptor from a different source               |
| `UrlPathHelper`                    | `@ConditionalOnMissingBean` | Override Spring's path resolution logic                                      |
