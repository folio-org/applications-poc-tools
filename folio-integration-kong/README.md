# folio-integration-kong

Kong API Gateway integration for registering, updating, and removing routes based on FOLIO `ModuleDescriptor` definitions. Supports Kong's expression-based routing (Kong 3.x+) and per-tenant route management.

## Features

- **Module registration** -- automatically registers a module as a Kong service with routes on startup
- **Route management** -- add, update, and remove routes derived from `ModuleDescriptor` routing entries
- **Tenant routing** -- add or remove tenants from Kong route expressions for per-tenant access control
- **Expression-based routing** -- builder DSL for Kong expression router language

## Configuration

```yaml
application:
  kong:
    enabled: true
    url: http://localhost:8001
    module-self-url: http://my-module:8081
    register-module: true
    retries: 5
    connect-timeout: 60000
    read-timeout: 60000
    write-timeout: 60000
    tls:
      enabled: false
      trust-store-path: /path/to/truststore.jks
      trust-store-password: secret
      trust-store-type: jks
```

## Key Classes

### Service Layer

- `KongGatewayService` -- core service for managing Kong services and routes
  - `addRoutes()`, `updateRoutes()`, `removeRoutes()` from `ModuleDescriptor` lists
  - `upsertService()`, `deleteService()`, `deleteServiceRoutes()`
  - `addTenantToModuleRoutes()`, `removeTenantFromModuleRoutes()`
- `KongModuleRegistrar` -- high-level registrar: creates a Kong service and its routes on startup
- `KongRouteTenantService` -- parses and mutates tenant regex in Kong route expressions

### Client

- `KongAdminClient` -- Feign client to Kong Admin API (services, routes CRUD, tag-based queries)

### Expression DSL (`org.folio.tools.kong.model.expression`)

- `RouteExpression`, `RouteExpressions` -- builder DSL for Kong route expressions
- Typed expression builders: `StringExpressionBuilder`, `IntExpressionBuilder`, `IpAddrExpressionBuilder`

## Usage

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-integration-kong</artifactId>
  <version>${applications-poc-tools.version}</version>
</dependency>
```
