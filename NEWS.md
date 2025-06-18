## Version `v3.1.0` (in progress)
### Changes:
* Validate semver when getting name or version from artifact id (MGRENTITLE-113)
* Replace confluentinc/cp-kafka with apache/kafka-native:3.8.0 ((APPPOCTOOL-55)[https://folio-org.atlassian.net/browse/APPPOCTOOL-55])
* Retry KafkaContainer startup if it fails ((APPPOCTOOL-57)[https://folio-org.atlassian.net/browse/APPPOCTOOL-57])
* TESTCONTAINERS\_POSTGRES\_IMAGE; PostgreSQL 16 ((APPPOCTOOL-56)[https://folio-org.atlassian.net/browse/APPPOCTOOL-56])
* Support "delete" operation in secure store (FSSP-1)
* SecretStore implementation for folio-secure-store-proxy (APPPOCTOOL-59)
* Fix JWT token decoding
