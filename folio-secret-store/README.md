# folio-secret-store

Pluggable secret store abstraction for FOLIO services, split into two sub-modules.

## Sub-modules

| Module                                                                   | Description                                                                                     |
|:-------------------------------------------------------------------------|:------------------------------------------------------------------------------------------------|
| [folio-secret-store-common](folio-secret-store-common/README.md)        | Core `SecureStore` interface and implementations for Ephemeral, AWS SSM, Vault, and FSSP backends |
| [folio-secret-store-starter](folio-secret-store-starter/README.md)      | Spring Boot auto-configuration that registers the correct `SecureStore` bean from a property    |

---

## Secure Store Backends

| Backend      | Type value   | Use case                                              |
|:-------------|:-------------|:------------------------------------------------------|
| `EphemeralStore` | `EPHEMERAL` | Development and testing — in-memory, no external service required |
| `AwsStore`       | `AWS_SSM`   | Production on AWS — credentials stored in SSM Parameter Store     |
| `VaultStore`     | `VAULT`     | Production with HashiCorp Vault                                   |
| `FsspStore`      | `FSSP`      | Production with Folio Secure Store Proxy                          |

---

## Spring Boot Autoconfiguration

The `folio-secret-store-starter` module provides autoconfiguration. The `SecureStoreAutoconfiguration`
class registers the `SecureStore` bean based on the `application.secret-store.type` property.
The default value is `EPHEMERAL`.

```yaml
application:
  secret-store:
    type: AWS_SSM
    environment: prod
    aws-ssm:
      region: eu-west-1
      use-iam: true
```

See [folio-secret-store-starter/README.md](folio-secret-store-starter/README.md) for the full
configuration reference for each store type.

---

## Direct Usage (without Spring Boot)

Use `SecureStoreFactory` to create a store from a `java.util.Properties` object outside a Spring
context:

```java
Properties props = new Properties();
props.load(new FileInputStream("vault.properties"));
SecureStore store = SecureStoreFactory.getSecureStore("Vault", props);
```

See [folio-secret-store-common/README.md](folio-secret-store-common/README.md) for the type
strings and per-backend property keys.
