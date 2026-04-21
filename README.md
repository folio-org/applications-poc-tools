# applications-poc-tools

Copyright (C) 2022-2025 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Library with general purpose classes to help with FOLIO backend development and testing.

Built on **Spring Boot 3.4.3** / **Java 17**.

## Modules

| Module | Description |
|--------|-------------|
| [folio-backend-common](folio-backend-common) | Core domain models, utility classes, and shared configuration properties |
| [folio-integration-kafka](folio-integration-kafka) | Kafka topic management, producer/consumer configuration, and tenant-aware message filtering |
| [folio-security](folio-security) | Spring Security integration supporting Okapi and Keycloak authorization flows |
| [folio-secret-store](folio-secret-store) | Pluggable secure credential storage with four backends (Ephemeral, AWS SSM, Vault, FSSP) |
| [folio-backend-testing](folio-backend-testing) | Testing infrastructure: JUnit 5 extensions, Testcontainers, WireMock, base test classes |
| [folio-integration-kong](folio-integration-kong) | Kong API Gateway integration for route registration and tenant management |
| [folio-tls-utils](folio-tls-utils) | TLS/SSL utilities for configuring secure HTTP clients |
| [folio-permission-utils](folio-permission-utils) | Utilities for parsing and classifying FOLIO permission strings |
| [folio-auth-openid](folio-auth-openid) | OpenID Connect / JWT parsing with multi-issuer support via SmallRye JWT |

## Usage

Add the required module(s) as Maven dependencies:

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-backend-common</artifactId>
  <version>${applications-poc-tools.version}</version>
</dependency>
```

See individual module READMEs for module-specific setup instructions.
