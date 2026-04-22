# folio-tls-utils

TLS/SSL utility classes for configuring secure HTTP clients from FOLIO's `TlsProperties`.

## Features

- Build TLS-enabled Feign/OkHttp clients from configuration properties
- Build TLS-enabled Java `HttpClient` instances
- JKS truststore loading and `SSLContext` construction
- FIPS mode detection

## Configuration

```yaml
tls:
  enabled: true
  trust-store-path: /path/to/truststore.jks
  trust-store-password: secret
  trust-store-type: jks
```

## Key Classes

- `TlsProperties` -- configuration properties: `enabled`, `trustStorePath`, `trustStorePassword`, `trustStoreType`
- `FeignClientTlsUtils` -- factory methods for building TLS-enabled HTTP clients:
  - `buildTargetFeignClient()` -- creates a Feign target with TLS
  - `getSslOkHttpClient()` -- builds an `OkHttpClient` with custom `SSLContext`
  - `getHttpClientBuilder()` -- builds a Java `HttpClient.Builder` with TLS
  - `buildSslContext()` -- loads a JKS truststore and builds an `SSLContext`
- `FipsChecker` -- detects whether the JVM is running in FIPS mode

## Usage

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-tls-utils</artifactId>
  <version>${applications-poc-tools.version}</version>
</dependency>
```
