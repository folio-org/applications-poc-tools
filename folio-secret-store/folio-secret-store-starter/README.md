# folio-secret-store-starter

Spring Boot auto-configuration for FOLIO secret stores. Automatically registers a `SecureStore`
bean based on the `application.secret-store.type` property. The default type is `EPHEMERAL`.

## Table of Contents

- [Activation](#activation)
- [Configuration](#configuration)
- [Supported Store Types](#supported-store-types)
- [Full Example](#full-example)

---

## Activation

Add the dependency and the starter activates automatically:

```xml
<dependency>
    <groupId>org.folio</groupId>
    <artifactId>folio-secret-store-starter</artifactId>
</dependency>
```

A single `SecureStore` bean is registered in the application context. Inject it anywhere:

```java
@RequiredArgsConstructor
@Service
public class CredentialService {
    private final SecureStore secureStore;

    public String getClientSecret(String key) {
        return secureStore.get(key);
    }
}
```

---

## Configuration

The shared `application.secret-store` namespace carries the type selector and the
environment name used as a key prefix:

| Property                            | Type     | Default      | Description                                           |
|:------------------------------------|:---------|:-------------|:------------------------------------------------------|
| `application.secret-store.type`     | `String` | `EPHEMERAL`  | Store backend: `EPHEMERAL`, `AWS_SSM`, `VAULT`, `FSSP` |
| `application.secret-store.environment` | `String` | —         | Key namespace prefix; must match `[\w\-]+`            |

---

## Supported Store Types

### `EPHEMERAL` (default)

In-memory store for development. Properties are loaded from
`application.secret-store.ephemeral.*`.

| Property                                        | Description                                  |
|:------------------------------------------------|:---------------------------------------------|
| `application.secret-store.ephemeral.content.*` | Key-value pairs pre-loaded into the store    |

### `AWS_SSM`

AWS Systems Manager Parameter Store. Properties are loaded from
`application.secret-store.aws-ssm.*`.

| Property                                              | Description                                                        |
|:------------------------------------------------------|:-------------------------------------------------------------------|
| `application.secret-store.aws-ssm.region`             | AWS region                                                         |
| `application.secret-store.aws-ssm.access-key`         | AWS access key (omit when using IAM role)                          |
| `application.secret-store.aws-ssm.secret-key`         | AWS secret key (omit when using IAM role)                          |
| `application.secret-store.aws-ssm.use-iam`            | `true` to rely on the current IAM role                             |
| `application.secret-store.aws-ssm.ecs-credentials-endpoint` | HTTP endpoint for ECS credential retrieval                  |
| `application.secret-store.aws-ssm.ecs-credentials-path`     | Path component of the ECS credentials endpoint URI          |

### `VAULT`

HashiCorp Vault. Properties are loaded from `application.secret-store.vault.*`.

| Property                                                 | Description                                           |
|:---------------------------------------------------------|:------------------------------------------------------|
| `application.secret-store.vault.token`                   | Vault access token                                    |
| `application.secret-store.vault.address`                 | Vault server address                                  |
| `application.secret-store.vault.enable-ssl`              | Whether to use TLS                                    |
| `application.secret-store.vault.pem-file-path`           | Path to an X.509 PEM certificate                      |
| `application.secret-store.vault.keystore-password`       | Keystore password (optional)                          |
| `application.secret-store.vault.keystore-file-path`      | Path to a JKS keystore (client certificate)           |
| `application.secret-store.vault.truststore-file-path`    | Path to a JKS truststore (trusted server certs)       |

### `FSSP`

Folio Secure Store Proxy. Properties are loaded from `application.secret-store.fssp.*`.

| Property                                                  | Description                               |
|:----------------------------------------------------------|:------------------------------------------|
| `application.secret-store.fssp.address`                   | FSSP server address                       |
| `application.secret-store.fssp.secret-path`               | Root secrets path; default: `secure-store/entries` |
| `application.secret-store.fssp.enable-ssl`                | Whether to use TLS; default: `false`      |
| `application.secret-store.fssp.trust-store-path`          | Truststore file path                      |
| `application.secret-store.fssp.trust-store-password`      | Truststore password                       |
| `application.secret-store.fssp.trust-store-file-type`     | Truststore type; default: `jks`           |

---

## Full Example

```yaml
application:
  secret-store:
    type: AWS_SSM
    environment: prod
    aws-ssm:
      region: eu-west-1
      use-iam: true
```

Development override (e.g. in `application-local.yml`):

```yaml
application:
  secret-store:
    type: EPHEMERAL
    environment: folio
    ephemeral:
      content:
        my-key: my-secret-value
```
