# folio-tls-utils

Reusable TLS/SSL infrastructure for FOLIO microservices. Provides truststore-backed `SSLContext`
creation, Spring HTTP Service Client proxy factories with optional TLS support, and FIPS mode
detection and diagnostics.

## Table of Contents

- [TLS Configuration](#tls-configuration)
- [Building HTTP Service Clients](#building-http-service-clients)
- [FIPS Detection](#fips-detection)
- [Exception Handling](#exception-handling)

---

## TLS Configuration

`TlsProperties` is a configuration POJO that carries the four fields required to configure a
truststore. Consuming modules bind it at their chosen prefix using
`@ConfigurationProperties` or `@EnableConfigurationProperties`.

```java
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "application.my-client.tls")
public class MyClientTlsProperties extends TlsProperties { }
```

```yaml
application:
  my-client:
    tls:
      enabled: true
      trust-store-path: classpath:certs/my-truststore.jks
      trust-store-password: changeit
      trust-store-type: JKS        # optional; defaults to KeyStore.getDefaultType()
```

| Field                | Type      | Description                                                          |
|:---------------------|:----------|:---------------------------------------------------------------------|
| `enabled`            | `boolean` | TLS customisation is applied only when `true`                        |
| `trustStorePath`     | `String`  | Filesystem or classpath path to the truststore file                  |
| `trustStorePassword` | `String`  | Truststore password; returns empty string instead of `null`          |
| `trustStoreType`     | `String`  | Keystore type; blank value falls back to `KeyStore.getDefaultType()` |

`TlsProperties` is used throughout the FOLIO library stack wherever an outbound HTTPS client needs
a custom trust root: `folio-security` (Keycloak admin client), `folio-integration-kong` (Kong Admin
API client), and all `@HttpExchange` service clients built via `HttpClientTlsUtils`.

---

## Building HTTP Service Clients

`HttpClientTlsUtils.buildHttpServiceClient` creates a Spring 6 `@HttpExchange` interface proxy
backed by a `RestClient`. When TLS is enabled, the client uses a `JdkClientHttpRequestFactory`
configured with the `SSLContext` derived from the truststore; otherwise it uses the JDK default
trust.

```java
@HttpExchange
public interface DownstreamClient {
    @GetExchange("/api/resource/{id}")
    Resource getResource(@PathVariable String id);
}
```

```java
// In a @Configuration class:
@Bean
public DownstreamClient downstreamClient(
        RestClient.Builder restClientBuilder,
        TlsProperties tls) {
    return HttpClientTlsUtils.buildHttpServiceClient(
        restClientBuilder,
        tls,
        "https://downstream-service:8443",
        DownstreamClient.class
    );
}
```

When `tls.isEnabled()` is `false` (or `tls` is `null`), a `SimpleClientHttpRequestFactory` is used
with no custom SSL configuration.

> **Truststore only.** The current implementation configures server certificate verification
> (trust) only. Mutual TLS (client certificates) is not supported.

### Hostname verification

The constant `Utils.IS_HOSTNAME_VERIFICATION_DISABLED` reflects the JDK system property
`jdk.internal.httpclient.disableHostnameVerification`. Set it at JVM startup to disable hostname
verification across all JDK HTTP clients:

```shell
-Djdk.internal.httpclient.disableHostnameVerification=true
```

---

## Building an SSLContext Directly

Use `Utils.buildSslContext(TlsProperties)` when you need a raw `SSLContext` for non-HTTP use cases
(e.g. configuring a Keycloak `ResteasyClient` or a custom Netty pipeline):

```java
SSLContext sslContext = Utils.buildSslContext(tlsProperties);
```

Throws `SslInitializationException` (unchecked) if the truststore cannot be loaded.

---

## FIPS Detection

`FipsChecker` provides reflection-based FIPS mode detection and diagnostics without a hard
compile-time dependency on BouncyCastle. It is safe to use on any JVM regardless of whether
BouncyCastle is on the classpath.

```java
// Log FIPS posture at startup
log.info(FipsChecker.getFipsChecksResultString());
```

`getFipsChecksResultString()` aggregates:

| Check                           | Method                               | Returns                                   |
|:--------------------------------|:-------------------------------------|:------------------------------------------|
| BouncyCastle approved-only mode | `isInBouncycastleApprovedOnlyMode()` | `"Enabled"`, `"Disabled"`, or `"Unknown"` |
| JDK system FIPS flag            | `isSystemFipsEnabled()`              | `"Enabled"`, `"Disabled"`, or `"Unknown"` |
| Registered security providers   | `dumpJavaSecurityProviders()`        | Multi-line provider list                  |
| SSL-related system properties   | `dumpSecurityProperties()`           | All SSL/keystore system properties        |

When BouncyCastle approved-only mode is active, use `FipsChecker.getApprovedSecureRandomSafe()` to
obtain a `SecureRandom` instance from the BCFIPS provider:

```java
SecureRandom random = FipsChecker.getApprovedSecureRandomSafe();
if (random == null) {
    // BCFIPS not active — use standard SecureRandom
    random = new SecureRandom();
}
```

---

## Exception Handling

`SslInitializationException` (extends `IllegalStateException`) is thrown by `Utils.buildSslContext`
and propagated through `HttpClientTlsUtils` when truststore loading fails. It wraps the original
checked cause, allowing callers to distinguish SSL setup failures from other runtime errors:

```java
try {
    SSLContext ctx = Utils.buildSslContext(tlsProperties);
} catch (SslInitializationException e) {
    log.error("Failed to initialize SSL context", e.getCause());
}
```
