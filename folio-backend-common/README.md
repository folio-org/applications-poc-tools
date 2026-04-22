# folio-backend-common

Core shared domain models, utility classes, and configuration properties used across FOLIO backend modules.

## Key Classes

### Domain Models (`org.folio.common.domain.model`)

- `ApplicationDescriptor`, `ModuleDescriptor`, `InterfaceDescriptor` -- FOLIO descriptor models
- `RoutingEntry`, `RoutingEntrySchedule` -- HTTP routing entry definitions
- `Capability`, `Permission`, `Dependency` -- authorization and dependency models
- `ResultList`, `SearchResult`, `OffsetRequest` -- pagination/search result wrappers
- `Error`, `ErrorCode`, `ErrorResponse`, `Parameter` -- standard FOLIO error response envelope

### Utilities (`org.folio.common.utils`)

- `OkapiHeaders` -- constants for all `x-okapi-*` header names (TOKEN, TENANT, URL, USER_ID, etc.)
- `CqlQuery` -- builder for CQL (Contextual Query Language) query strings
- `SemverUtils` -- semantic version comparison via semver4j
- `CollectionUtils`, `Collectors` -- stream/collection helpers
- `PaginationUtils` -- pagination calculation helpers
- `UuidUtils` -- UUID generation/validation

### Configuration (`org.folio.common.configuration.properties`)

- `FolioEnvironment` -- Spring configuration property binding for the FOLIO environment

### Services (`org.folio.common.service`)

- `TransactionHelper` -- helper for programmatic Spring transaction management

## Usage

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-backend-common</artifactId>
  <version>${applications-poc-tools.version}</version>
</dependency>
```
