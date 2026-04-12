# folio-secret-store-common

Core secret store abstraction for FOLIO services. Defines the `SecureStore` interface and provides
implementations for four backends: Ephemeral (development), AWS SSM, Vault, and FSSP. Use this
module directly when wiring stores outside a Spring Boot context; for Spring Boot applications
prefer `folio-secret-store-starter`, which auto-configures the correct store based on a property.

## Table of Contents

- [Interface](#interface)
- [Implementations](#implementations)
- [Factory](#factory)
- [Configuration Properties](#configuration-properties)

---

## Interface

`SecureStore` is a simple key-value contract with four operations.

| Method               | Description                                                                             |
|:---------------------|:----------------------------------------------------------------------------------------|
| `get(key)`           | Returns the value; throws `SecretNotFoundException` if the key does not exist           |
| `set(key, value)`    | Stores a value for the key                                                              |
| `delete(key)`        | Removes the key; idempotent — no-op if the key does not exist                          |
| `lookup(key)`        | Returns `Optional<String>` — empty when the key is not found (wraps `get` internally)  |

```java
SecureStore store = SecureStoreFactory.getSecureStore("Vault", props);

// get or throw
String secret = store.get("my-key");

// get or empty Optional
Optional<String> maybeSecret = store.lookup("optional-key");

// set
store.set("my-key", "new-value");

// delete (idempotent)
store.delete("old-key");
```

---

## Implementations

### `EphemeralStore` (type: `"Ephemeral"`)

In-memory store intended for **development and testing only**. Secrets are pre-loaded from a
properties map passed at construction time.

| Property      | Description                                                                               |
|:--------------|:------------------------------------------------------------------------------------------|
| `tenants`     | Comma-separated list of tenant IDs                                                        |
| `{tenantId}`  | Credentials string for the tenant: `tenantId:keycloakClientId:keycloakClientSecret`      |

### `AwsStore` (type: `"AwsSsm"`)

Retrieves and stores credentials in AWS Systems Manager Parameter Store, encrypted with a KMS key.

**Key format:** `{salt}_{tenantId}_{username}` — e.g. `ab73kbw90e_diku_diku`

| Property                 | Description                                                                |
|:-------------------------|:---------------------------------------------------------------------------|
| `region`                 | AWS region for the SSM client                                              |
| `accessKey`              | AWS access key (omit when using IAM role)                                  |
| `secretKey`              | AWS secret key (omit when using IAM role)                                  |
| `useIam`                 | `true` to rely on the current IAM role instead of explicit credentials     |
| `ecsCredentialsEndpoint` | HTTP endpoint for ECS credential retrieval                                 |
| `ecsCredentialsPath`     | Path component of the ECS credentials endpoint URI                         |

### `VaultStore` (type: `"Vault"`)

Retrieves credentials from a [HashiCorp Vault](https://vaultproject.io) instance.

**Key format:** `{salt}/{tenantId}` (path), `{username}` (field) — e.g. key `ab73kbw90e/diku`, field `diku`

| Property                  | Description                                                              |
|:--------------------------|:-------------------------------------------------------------------------|
| `token`                   | Vault token (may be a root token)                                        |
| `address`                 | Vault server address                                                     |
| `enableSSL`               | Whether to use TLS                                                       |
| `ssl.pem.path`            | Path to an X.509 PEM certificate                                         |
| `ssl.truststore.jks.path` | Path to a JKS truststore with trusted Vault server certificates          |
| `ssl.keystore.jks.path`   | Path to a JKS keystore containing a client certificate and private key   |
| `ssl.keystore.password`   | Password for the JKS keystore (optional)                                 |

### `FsspStore` (type: `"Fssp"`)

Retrieves credentials by key from a Folio Secure Store Proxy (FSSP).

**Key format:** arbitrary string key — e.g. `service-key`

| Property           | Description                                                     |
|:-------------------|:----------------------------------------------------------------|
| `address`          | FSSP server address                                             |
| `secretPath`       | Root path for secrets; default: `secure-store/entries`          |
| `enableSsl`        | Whether to use TLS; default: `false`                            |
| `trustStorePath`   | Path to the TLS truststore file                                 |
| `trustStorePassword` | Truststore password                                           |
| `trustStoreFileType` | Truststore type (`jks`, `pem`, etc.); default: `jks`          |

---

## Factory

`SecureStoreFactory.getSecureStore(type, properties)` creates the correct `SecureStore`
implementation from a `java.util.Properties` object. This is the non-Spring wiring path.

| Type string    | Implementation     |
|:---------------|:-------------------|
| `"Ephemeral"`  | `EphemeralStore`   |
| `"AwsSsm"`     | `AwsStore`         |
| `"Vault"`      | `VaultStore`       |
| `"Fssp"`       | `FsspStore`        |
| anything else  | `EphemeralStore`   |

```java
Properties props = new Properties();
props.load(new FileInputStream("vault.properties"));
SecureStore store = SecureStoreFactory.getSecureStore("Vault", props);
```

---

## Configuration Properties

`SecureStoreConfigProperties` is a plain POJO with a single `environment` field, used as the base
class for all backend-specific property classes. It holds the environment name (key namespace
prefix) for backends that namespace secrets by environment (e.g. AWS SSM).
