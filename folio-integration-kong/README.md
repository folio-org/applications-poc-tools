# folio-integration-kong

Spring Boot auto-configuration library for integrating FOLIO modules with
[Kong API Gateway](https://konghq.com/) 3.x. Translates FOLIO module descriptors into Kong
Services and expression-router Routes, manages per-tenant route access control, and optionally
self-registers the hosting module on startup.

## Table of Contents

- [Activation](#activation)
- [Configuration](#configuration)
- [Route Management](#route-management)
- [Tenant Route Access Control](#tenant-route-access-control)
- [Module Self-Registration](#module-self-registration)
- [Route Expression DSL](#route-expression-dsl)
- [Error Handling](#error-handling)

---

## Activation

The library activates automatically via Spring Boot auto-configuration when
`application.kong.enabled=true` is set. No annotation is required.

```yaml
application:
  kong:
    enabled: true
    url: http://kong-admin:8001
```

---

## Configuration

| Property                                    | Type      | Default | Description                                                                 |
|:--------------------------------------------|:----------|:--------|:----------------------------------------------------------------------------|
| `application.kong.enabled`                  | `Boolean` | `false` | Master on/off switch                                                        |
| `application.kong.url`                      | `String`  | —       | Kong Admin API base URL                                                     |
| `application.kong.module-self-url`          | `String`  | —       | Upstream URL of the current module (used for self-registration)             |
| `application.kong.register-module`          | `Boolean` | `false` | Self-register on startup from `classpath:descriptors/ModuleDescriptor.json` |
| `application.kong.retries`                  | `Integer` | —       | Proxy retries on the Kong Service object                                    |
| `application.kong.connect-timeout`          | `Integer` | —       | Connection timeout in ms from Kong to upstream                              |
| `application.kong.write-timeout`            | `Integer` | —       | Write timeout in ms                                                         |
| `application.kong.read-timeout`             | `Integer` | —       | Read timeout in ms                                                          |
| `application.kong.tls.enabled`              | `boolean` | `false` | Enable TLS for Kong Admin API calls                                         |
| `application.kong.tls.trust-store-path`     | `String`  | —       | Truststore file path                                                        |
| `application.kong.tls.trust-store-password` | `String`  | —       | Truststore password                                                         |
| `application.kong.tls.trust-store-type`     | `String`  | —       | Truststore type (e.g. `JKS`, `PKCS12`)                                      |

---

## Route Management

`KongGatewayService` is the main orchestration service. It translates FOLIO `ModuleDescriptor`
objects into Kong Services and Routes.

### Add routes for modules

Creates a Kong Service and Routes for each module descriptor. Route names are derived from a SHA-1
hash of the path, methods, module ID, and interface ID, making them deterministic and idempotent.

```java
@RequiredArgsConstructor
public class ModuleRegistrationService {
    private final KongGatewayService kongGatewayService;

    public void registerModules(Collection<ModuleDescriptor> descriptors) {
        kongGatewayService.addRoutes(descriptors);
    }
}
```

### Update routes (diff-based sync)

Computes the diff between the desired routes (from the descriptor) and the existing routes on Kong
(fetched by tag), creating new ones and deleting deprecated ones.

```java
kongGatewayService.updateRoutes(descriptors);
```

### Remove routes for modules

Deletes all Kong routes tagged with the module ID, then removes the service.

```java
kongGatewayService.removeRoutes(descriptors);
```

### Route structure

Each route created by `KongGatewayService` uses Kong's [expression router](https://docs.konghq.com/gateway/latest/reference/router-expressions-language/):

- FOLIO URL path variables `{param}` are converted to the regex group `([^/]+)`.
- Wildcard `*` is converted to `(.*)`.
- Static (no variable) paths get priority `1`; regex paths get priority `0`.
- Routes for non-manager-component modules include an `x-okapi-tenant` header wildcard placeholder
  that is narrowed to specific tenants as they are enabled (see below).

---

## Tenant Route Access Control

When a tenant is entitled to use a module, its routes are narrowed to accept only that tenant's
requests. `KongGatewayService` delegates the expression rewriting to `KongRouteTenantService`.

### Add a tenant to module routes

```java
kongGatewayService.addTenantToModuleRoutes("mod-inventory-1.0.0", "diku");
```

The `x-okapi-tenant` wildcard expression `http.headers.x_okapi_tenant ~ r#".*"#` is replaced with
an exact equality clause. Subsequent tenants are appended with `||`:

```
Before: ... && (http.headers.x_okapi_tenant ~ r#".*"#)
After:  ... && (http.headers.x_okapi_tenant == "diku")
After 2nd tenant: ... && (http.headers.x_okapi_tenant == "diku" || http.headers.x_okapi_tenant == "tenant2")
```

### Remove a tenant from module routes

```java
kongGatewayService.removeTenantFromModuleRoutes("mod-inventory-1.0.0", "diku");
```

When the last tenant is removed, the clause reverts to the wildcard placeholder rather than leaving
the route tenant-less.

---

## Module Self-Registration

When `application.kong.register-module=true`, `KongModuleRegistrar` fires on
`ApplicationReadyEvent` and:

1. Reads `classpath:descriptors/ModuleDescriptor.json`.
2. Upserts a Kong Service pointing at `application.kong.module-self-url` with the configured
   timeouts and retry settings.
3. Calls `updateRoutes` to sync the declared routes with Kong.

This is the zero-touch self-registration path for modules that manage their own Kong presence.

---

## Route Expression DSL

`folio-integration-kong` ships a fluent DSL for building Kong 3.x expression-router predicates.
The entry point is the `RouteExpressions` interface (all static factory methods).

### Basic predicates

```java
import static org.folio.tools.kong.model.expression.RouteExpressions.*;

// HTTP method
RouteExpression methodGet = httpMethod().equalsTo("GET");

// URL path (regex)
RouteExpression pathExpr = httpPath().regexMatching("^/inventory/items/([^/]+)$");

// Header value
RouteExpression tenantExpr = httpHeader("X-Okapi-Tenant").equalsTo("diku");

// Port
RouteExpression portExpr = netPort().greaterThan(1024);

// IP address
RouteExpression ipExpr = netSrcIp().in("10.0.0.0/8");
```

### Combining predicates

```java
// Logical AND / OR
RouteExpression combined = bool(methodGet).and(pathExpr);
RouteExpression either   = bool(methodGet).or(httpMethod().equalsTo("HEAD"));

// Helpers for N expressions
RouteExpression allOf = combineUsingAnd(methodGet, pathExpr, tenantExpr);
RouteExpression anyOf = combineUsingOr(methodGet, httpMethod().equalsTo("POST"));
```

### Case-insensitive header matching

```java
// lower() transformation for case-insensitive comparison
RouteExpression expr = httpHeader("X-Okapi-Tenant")
    .withTransformation(StringTransformations.LOWER)
    .equalsTo("diku");
```

### Setting an expression on a Route

```java
Route route = new Route()
    .expression(combineUsingAnd(methodGet, pathExpr, tenantExpr))
    .priority(1)
    .tags(List.of("mod-inventory-1.0.0"));
```

---

## Error Handling

`KongIntegrationException` is thrown when bulk route create/update/delete operations encounter
partial failures. It carries a `List<Parameter>` of structured error details describing which
routes or services failed and why.

`TenantRouteUpdateException` is thrown when adding or removing a tenant from module routes fails.

Both exceptions are unchecked (`RuntimeException`) and should be caught by the caller to implement
retry or rollback logic.
