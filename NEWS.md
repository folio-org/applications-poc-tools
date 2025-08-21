## Version `v3.1.0` (in progress)
### Changes:
* Support find topics by name in KafkaAdminService (MGRENTITLE-135)
* Validate semver when getting name or version from artifact id (MGRENTITLE-113)
* Replace confluentinc/cp-kafka with apache/kafka-native:3.8.0 ((APPPOCTOOL-55)[https://folio-org.atlassian.net/browse/APPPOCTOOL-55])
* Retry KafkaContainer startup if it fails ((APPPOCTOOL-57)[https://folio-org.atlassian.net/browse/APPPOCTOOL-57])
* TESTCONTAINERS\_POSTGRES\_IMAGE; PostgreSQL 16 ((APPPOCTOOL-56)[https://folio-org.atlassian.net/browse/APPPOCTOOL-56])
* Support "delete" operation in secure store (FSSP-1)
* SecretStore implementation for folio-secure-store-proxy (APPPOCTOOL-59)
* Fix JWT token decoding
* Fix: `.collection.post` is DATA not PROCEDURAL
* Use SECURE\_STORE\_ENV, not ENV, for secure store key; drop KeycloakSecretUtils ((APPPOCTOOL-63)[https://folio-org.atlassian.net/browse/APPPOCTOOL-63])

### Migration:
* Replace KeycloakSecretUtils with KeycloakStoreKeyProvider, add `application.secure-store.environment=${SECURE_STORE_ENV:folio}` to application.yaml (for Ramsons and Sunflower: add `application.secure-store.environment=${SECURE_STORE_ENV:${ENV:folio}}` to application.yaml).
