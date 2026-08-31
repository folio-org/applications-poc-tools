# folio-integration-kong

Spring Boot auto-configuration library for integrating FOLIO modules with the API Gateway, currently
implemented against [Kong](https://konghq.com/) 3.x. Translates FOLIO module descriptors into Kong
Services and expression-router Routes, manages per-tenant route access control, and optionally
self-registers the hosting module on startup.

## Table of Contents

- [Activation](#activation)
- [Configuration](#configuration)
- [Deprecated configuration](#deprecated-configuration)
- [Route Management](#route-management)
- [Tenant Route Access Control](#tenant-route-access-control)
- [Module Self-Registration](#module-self-registration)
- [Route Expression DSL](#route-expression-dsl)
- [Error Handling](#error-handling)

---

## Activation

The library activates automatically via Spring Boot auto-configuration when
`application.apigw.enabled=true` is set. No annotation is required.

```yaml
application:
  apigw:
    enabled: true
    url: http://kong-admin:8001
```

---

## Configuration

| Property                                     | Type      | Default | Description                                                                 |
|:---------------------------------------------|:----------|:--------|:----------------------------------------------------------------------------|
| `application.apigw.enabled`                  | `Boolean` | `false` | Master on/off switch                                                        |
| `application.apigw.url`                      | `String`  | —       | Kong Admin API base URL                                                     |
| `application.apigw.module-self-url`          | `String`  | —       | Upstream URL of the current module (used for self-registration)             |
| `application.apigw.register-module`          | `Boolean` | `false` | Self-register on startup from `classpath:descriptors/ModuleDescriptor.json` |
| `application.apigw.retries`                  | `Integer` | —       | Proxy retries on the Kong Service object                                    |
| `application.apigw.connect-timeout`          | `Integer` | —       | Connection timeout in ms from the gateway to upstream                       |
| `application.apigw.write-timeout`            | `Integer` | —       | Write timeout in ms                                                         |
| `application.apigw.read-timeout`             | `Integer` | —       | Read timeout in ms                                                          |
| `application.apigw.tls.enabled`              | `boolean` | `false` | Enable TLS for Kong Admin API calls                                         |
| `application.apigw.tls.trust-store-path`     | `String`  | —       | Truststore file path                                                        |
| `application.apigw.tls.trust-store-password` | `String`  | —       | Truststore password                                                         |
| `application.apigw.tls.trust-store-type`     | `String`  | —       | Truststore type (e.g. `JKS`, `PKCS12`)                                      |

---

## Deprecated configuration

The gateway-specific `application.kong.*` properties and `KONG_*` environment variables still work,
but they are deprecated. `DeprecatedKongPropertiesPostProcessor` logs one `WARN` line per legacy name
in use at startup and never changes the resolved values. Removal is planned for the **Vetch** flower
release.

When a legacy name and its replacement are both set, the replacement wins and the `WARN` line says so.

| Deprecated property                         | Replacement                                  |
|:--------------------------------------------|:---------------------------------------------|
| `application.kong.enabled`                  | `application.apigw.enabled`                  |
| `application.kong.url`                      | `application.apigw.url`                      |
| `application.kong.module-self-url`          | `application.apigw.module-self-url`          |
| `application.kong.register-module`          | `application.apigw.register-module`          |
| `application.kong.retries`                  | `application.apigw.retries`                  |
| `application.kong.connect-timeout`          | `application.apigw.connect-timeout`          |
| `application.kong.write-timeout`            | `application.apigw.write-timeout`            |
| `application.kong.read-timeout`             | `application.apigw.read-timeout`             |
| `application.kong.tls.enabled`              | `application.apigw.tls.enabled`              |
| `application.kong.tls.trust-store-path`     | `application.apigw.tls.trust-store-path`     |
| `application.kong.tls.trust-store-password` | `application.apigw.tls.trust-store-password` |
| `application.kong.tls.trust-store-type`     | `application.apigw.tls.trust-store-type`     |

| Deprecated environment variable | Replacement                    |
|:--------------------------------|:-------------------------------|
| `KONG_INTEGRATION_ENABLED`      | `APIGW_ENABLED`                |
| `KONG_ADMIN_URL`                | `APIGW_URL`                    |
| `REGISTER_MODULE_IN_KONG`       | `APIGW_REGISTER_MODULE`        |
| `KONG_RETRIES`                  | `APIGW_RETRIES`                |
| `KONG_CONNECT_TIMEOUT`          | `APIGW_CONNECT_TIMEOUT`        |
| `KONG_READ_TIMEOUT`             | `APIGW_READ_TIMEOUT`           |
| `KONG_WRITE_TIMEOUT`            | `APIGW_WRITE_TIMEOUT`          |
| `KONG_TLS_ENABLED`              | `APIGW_TLS_ENABLED`            |
| `KONG_TLS_TRUSTSTORE_PATH`      | `APIGW_TLS_TRUSTSTORE_PATH`    |
| `KONG_TLS_TRUSTSTORE_PASSWORD`  | `APIGW_TLS_TRUSTSTORE_PASSWORD`|
| `KONG_TLS_TRUSTSTORE_TYPE`      | `APIGW_TLS_TRUSTSTORE_TYPE`    |
| `KONG_TENANT_CHECKS_ENABLED`    | `APIGW_TENANT_CHECKS_ENABLED`  |
| `APPLICATION_KONG_<suffix>`     | `APPLICATION_APIGW_<suffix>`   |

Kong-the-product container variables such as `KONG_PG_*` and `KONG_PROXY_*`, and `MODULE_URL`, are
not deprecated and never reported.

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

When `application.apigw.register-module=true`, `ApiGatewayModuleRegistrar` fires on
`ApplicationReadyEvent` and:

1. Reads `classpath:descriptors/ModuleDescriptor.json`.
2. Upserts a Kong Service pointing at `application.apigw.module-self-url` with the configured
   timeouts and retry settings.
3. Calls `updateRoutes` to sync the declared routes with Kong.

This is the zero-touch self-registration path for modules that manage their own gateway presence.

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
