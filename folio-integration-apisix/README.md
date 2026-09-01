# folio-integration-apisix

Apache APISIX integration library for FOLIO Eureka: manages APISIX services and routes for FOLIO
modules through the [APISIX Admin API](https://apisix.apache.org/docs/apisix/admin-api/), implementing
the gateway-agnostic `org.folio.common.gateway.ApiGatewayService` contract from `folio-backend-common`
(the APISIX counterpart of `folio-integration-kong`).

---

## Activation

The library activates automatically via Spring Boot auto-configuration when `application.apigw.enabled=true`
**and** `application.apigw.type=apisix` are set. No annotation is required. With `type=kong` (or unset),
`folio-integration-kong` is active instead — the two libraries are mutually exclusive at runtime.

```yaml
application:
  apigw:
    enabled: true
    type: apisix
    url: http://apisix:9180
    api-key: ${APISIX_ADMIN_KEY}
```

---

## Configuration

| Property                                     | Type      | Default | Description                                                                  |
|:---------------------------------------------|:----------|:--------|:-----------------------------------------------------------------------------|
| `application.apigw.enabled`                  | `Boolean` | `false` | Master on/off switch                                                         |
| `application.apigw.type`                     | `String`  | `kong`  | Active gateway implementation; this library is active only for `apisix`      |
| `application.apigw.url`                      | `String`  | —       | APISIX Admin API base URL (origin only, e.g. `http://apisix:9180`)           |
| `application.apigw.api-key`                  | `String`  | —       | APISIX Admin API key, sent as the `X-API-KEY` header                         |
| `application.apigw.module-self-url`          | `String`  | —       | Upstream URL of the current module (used for self-registration)              |
| `application.apigw.register-module`          | `Boolean` | `false` | Self-register on startup from `classpath:descriptors/ModuleDescriptor.json`  |
| `application.apigw.retries`                  | `Integer` | —       | Proxy retries on the APISIX upstream object                                  |
| `application.apigw.connect-timeout`          | `Integer` | —       | Connection timeout in **ms** (converted to seconds on the APISIX upstream)   |
| `application.apigw.write-timeout`            | `Integer` | —       | Write (send) timeout in **ms**                                               |
| `application.apigw.read-timeout`             | `Integer` | —       | Read timeout in **ms**                                                       |
| `application.apigw.tls.*`                    | —         | —       | TLS settings for Admin API calls (same shape as folio-integration-kong)      |

---

## Route model mapping

FOLIO module descriptors are translated to APISIX resources as follows:

| FOLIO concept | APISIX resource |
|:--|:--|
| Module | `service` — id = module id, upstream `roundrobin` with a single node parsed from the discovery URL, `pass_host: pass`; a path component in the discovery URL becomes a service-level `proxy-rewrite` prefixing the upstream path |
| Routing entry (static path) | `route` — exact `uri`, `priority: 1` |
| Routing entry (`{param}`/`*` pattern) | `route` — prefix-wildcard `uri` + anchored regex condition `["uri","~~","^…$"]` in `vars`, `priority: 0` |
| Interface of type `multiple` | `vars` condition `["http_x_okapi_module_id","==","<moduleId>"]` |
| Tenant check placeholder (non-`mgr-` modules) | `vars` condition `["http_x_okapi_tenant","~~",".*"]` (header must be present) |
| Tenant narrowing | wildcard replaced by `["http_x_okapi_tenant","in",["t1",…]]`; removing the last tenant restores the wildcard |
| Route identity | id = `sha1Hex(path\|methods\|moduleId\|interfaceId)` — deterministic and idempotent |
| Module/interface tagging | `labels`: `{module: <moduleId>, interface: <interfaceId>}` |

`updateRoutes` diffs desired against existing route ids: missing routes are created, stale ones deleted,
and unchanged existing routes are **never** re-submitted, so per-tenant `vars` state survives module upgrades.
Only `addTenantToModuleRoutes` / `removeTenantFromModuleRoutes` rewrite existing routes, and they change only
the tenant condition. Module routes carry no `plugins` — folio-apisix restricts the enabled plugin list.

System interfaces (`interfaceType: system`) are skipped. Bulk operations collect per-item failures and throw
a single `ApisixIntegrationException` (a subtype of `ApiGatewayIntegrationException`) carrying error parameters.
