# folio-auth-openid

JWT parsing and validation library for FOLIO services using OpenID Connect. Validates Keycloak-issued
JWTs against the issuer's JWKS endpoint, supports multi-realm (multi-tenant) token parsing with a
per-realm parser cache, handles automatic signing key rotation, and optionally allows the JWKS fetch
URL to differ from the public issuer URL (for in-cluster deployments).

## Table of Contents

- [Overview](#overview)
- [Usage](#usage)
- [Configuration](#configuration)
- [Multi-Tenant Support](#multi-tenant-support)
- [Key Rotation](#key-rotation)
- [Internal JWKS URL Override](#internal-jwks-url-override)
- [API Reference](#api-reference)

---

## Overview

The library is framework-neutral — it has no Spring Boot auto-configuration and no `@Component`
annotations. Consuming Spring applications wire the three classes manually in a `@Configuration`
class. Non-Spring applications can use them directly.

JWT validation is performed by [SmallRye JWT](https://github.com/smallrye/smallrye-jwt), which
implements MicroProfile JWT and handles JWKS fetching, key caching, signature verification, and
claims extraction.

---

## Usage

### 1. Configure and create the parser

```java
@Configuration
@RequiredArgsConstructor
public class JwtConfiguration {

    @Value("${application.keycloak.url}")
    private String keycloakUrl;

    @Bean
    public JwtParserConfiguration jwtParserConfiguration() {
        return JwtParserConfiguration.builder()
            .issuerRootUri(keycloakUrl)
            .validateUri(true)          // reject tokens from unexpected issuers
            .build();
    }

    @Bean
    public OpenidJwtParserProvider openidJwtParserProvider() {
        return new OpenidJwtParserProvider(
            60,   // jwksRefreshInterval (seconds)
            60    // forcedJwksRefreshInterval (seconds)
        );
    }

    @Bean
    public JsonWebTokenParser jsonWebTokenParser(
            ObjectMapper objectMapper,
            JwtParserConfiguration config,
            OpenidJwtParserProvider provider) {
        return new JsonWebTokenParser(objectMapper, config, provider);
    }
}
```

### 2. Parse and validate a token

```java
@RequiredArgsConstructor
public class TokenValidator {
    private final JsonWebTokenParser jwtParser;

    public JsonWebToken validate(String bearerToken) throws ParseException {
        return jwtParser.parse(bearerToken);
    }
}
```

`JsonWebToken` is SmallRye's claims interface — use it to access standard and custom claims:

```java
JsonWebToken jwt = jwtParser.parse(token);

String subject  = jwt.getSubject();
String issuer   = jwt.getIssuer();
String userId   = jwt.getClaim("user_id");
```

`ParseException` is thrown for any of the following conditions (see the message string to
distinguish them):

| Message                                        | Cause                                                                              |
|:-----------------------------------------------|:-----------------------------------------------------------------------------------|
| `"Invalid amount of segments in JsonWebToken"` | Token is not a valid compact JWS (not 2 or 3 dot-separated segments)               |
| `"Failed to decode json web token"`            | Payload Base64 decoding or JSON parsing failed                                     |
| `"Issuer not found in the json web token"`     | The `iss` claim is missing                                                         |
| `"Invalid JsonWebToken issuer"`                | Issuer fails the root URI prefix check, or no parser is registered for that issuer |

---

## Configuration

`JwtParserConfiguration` carries two settings:

| Field           | Type      | Description                                                                                                                                                    |
|:----------------|:----------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `issuerRootUri` | `String`  | Trusted issuer root URL (e.g. `https://keycloak.example.org`). All token issuers must start with this prefix when validation is enabled.                       |
| `validateUri`   | `boolean` | When `true`, the `iss` claim is checked against `issuerRootUri`. Set to `false` to accept tokens from any issuer (e.g. in tests or multi-cluster deployments). |

```java
JwtParserConfiguration config = JwtParserConfiguration.builder()
    .issuerRootUri("https://keycloak.example.org")
    .validateUri(true)
    .build();
```

---

## Multi-Tenant Support

`OpenidJwtParserProvider` maintains a `ConcurrentHashMap<String, JWTParser>` keyed by the full
issuer URI. For Keycloak, the issuer follows the pattern
`{keycloakBaseUrl}/realms/{realmName}` where the realm name is the FOLIO tenant identifier.

One parser instance per realm is created lazily on the first token parse and reused for all
subsequent tokens from that realm. This means a single application instance can validate tokens
from multiple Keycloak realms simultaneously.

### Cache invalidation

Invalidate the parser cache when tenants are disabled or removed:

```java
// Invalidate a specific realm
openidJwtParserProvider.invalidateCache("https://keycloak.example.org/realms/diku");

// Keep only active tenants, remove all others
openidJwtParserProvider.invalidateCache(List.of("diku", "tenant2"));

// Clear everything
openidJwtParserProvider.invalidateCache();
```

---

## Key Rotation

When Keycloak rotates its signing key, the `kid` in incoming tokens changes. SmallRye JWT throws
`UnresolvableKeyException` when it cannot find the new key in its cached JWKS. `JsonWebTokenParser`
catches this, evicts the stale parser for that issuer, and rethrows the `ParseException`. The next
parse call for that issuer creates a fresh parser that re-fetches the JWKS — no application restart
required.

---

## Internal JWKS URL Override

In Kubernetes deployments, the JWT `iss` claim typically contains the public-facing Keycloak URL
(e.g. `https://keycloak.company.com/realms/diku`), but the service must reach Keycloak via an
internal cluster hostname (e.g. `http://keycloak-headless:8080`). Use the three-argument
constructor to provide the internal base URL:

```java
new OpenidJwtParserProvider(
    60,                              // jwksRefreshInterval
    60,                              // forcedJwksRefreshInterval
    "http://keycloak-headless:8080"  // internal JWKS base URL
);
```

The library will validate the token `iss` against `issuerRootUri` (public URL) while fetching the
JWKS from the internal URL.

---

## API Reference

### `JsonWebTokenParser`

| Method                           | Description                                                                                               |
|:---------------------------------|:----------------------------------------------------------------------------------------------------------|
| `parse(String accessToken)`      | Validates the token and returns `JsonWebToken`. Throws `ParseException` on any validation failure.        |
| `INVALID_SEGMENTS_JWT_ERROR_MSG` | Public constant for the invalid-segments error message string, useful for matching in exception handlers. |

### `OpenidJwtParserProvider`

| Method                                        | Description                                                                        |
|:----------------------------------------------|:-----------------------------------------------------------------------------------|
| `getParser(String issuerUri)`                 | Returns a cached `JWTParser` for the given issuer URI, creating one on first call. |
| `invalidateCache()`                           | Clears all cached parsers.                                                         |
| `invalidateCache(String issuerUri)`           | Removes the cached parser for a specific issuer.                                   |
| `invalidateCache(Collection<String> tenants)` | Removes cached parsers whose realm name is NOT in the provided collection.         |

### `JwtParserConfiguration`

Built via Lombok `@Builder`. Fields: `issuerRootUri` (`String`), `validateUri` (`boolean`).
