# applications-poc-tools

Run this manual workflow after Maven API or dependency changes to confirm downstream services still build.
See [docs/workflows/verify-dependent-modules.md](docs/workflows/verify-dependent-modules.md) for quick instructions.

Copyright (C) 2022-2025 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Library with general purpose classes to help with FOLIO backend development and testing

## Modules

| Module                                                       | Description                                                                                                       |
|:-------------------------------------------------------------|:------------------------------------------------------------------------------------------------------------------|
| [folio-backend-common](folio-backend-common/README.md)       | Shared domain model, error types, validation constraints, pagination utilities, and common helpers                |
| [folio-backend-testing](folio-backend-testing/README.md)     | Reusable JUnit 5 extensions, Testcontainers wrappers, WireMock support, Kafka utilities, and MockMvc base classes |
| [folio-auth-openid](folio-auth-openid/README.md)             | JWT parsing and validation against Keycloak JWKS with multi-tenant parser cache and key rotation support          |
| [folio-security](folio-security/README.md)                   | Spring Security filter chain supporting Keycloak (JWT + UMA) and Okapi (`mod-authtoken`) authorization backends   |
| [folio-integration-kafka](folio-integration-kafka/README.md) | Kafka topic management: declarative startup creation, programmatic lifecycle, and multi-tenant naming conventions |
| [folio-integration-kong](folio-integration-kong/README.md)   | Kong API Gateway integration: route lifecycle management, tenant access control, and expression DSL               |
| [folio-secret-store](folio-secret-store/README.md)           | Pluggable secret store abstraction with AWS SSM, Vault, and environment-variable backends                         |
| [folio-tls-utils](folio-tls-utils/README.md)                 | TLS/SSL infrastructure: truststore-backed `SSLContext`, Spring HTTP Service Client factory, and FIPS detection    |
| [folio-permission-utils](folio-permission-utils/README.md)   | FOLIO permission name parser: classifies dot-separated names into type, action, and resource triples              |
