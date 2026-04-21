# folio-auth-openid

OpenID Connect / JWT parsing using SmallRye JWT. Handles multi-issuer JWT validation by extracting the `iss` claim and routing to the correct JWKS-backed parser.

## Features

- Parse JWT tokens to MicroProfile `JsonWebToken` objects
- Multi-issuer support with per-issuer JWKS parser caching
- Automatic cache invalidation on key resolution failures
- Optional issuer URI prefix validation

## Configuration

```java
var config = JwtParserConfiguration.builder()
    .issuerRootUri("https://keycloak.example.com/realms/")
    .validateUri(true)
    .build();

var parser = new JsonWebTokenParser(objectMapper, config, parserProvider);
JsonWebToken token = parser.parse(accessToken);
```

## Key Classes

- `JsonWebTokenParser` -- main entry point: extracts the issuer from the token payload, validates it, and delegates to the per-issuer parser
- `OpenidJwtParserProvider` -- manages a cache of per-issuer SmallRye JWT parsers backed by JWKS endpoints
- `JwtParserConfiguration` -- configuration: `issuerRootUri` and `validateUri` flag

## Usage

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-auth-openid</artifactId>
  <version>${applications-poc-tools.version}</version>
</dependency>
```
