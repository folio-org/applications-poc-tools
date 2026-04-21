# folio-permission-utils

Utility for parsing and classifying FOLIO permission strings into structured data.

## Features

Parses dot-separated FOLIO permission names (e.g., `foo.items.item.post`) into a `PermissionData` object containing:

- **Resource** -- human-readable resource name derived from the permission segments
- **Type** -- `DATA`, `SETTINGS`, or `PROCEDURAL`
- **Action** -- `VIEW`, `CREATE`, `EDIT`, `DELETE`, `MANAGE`, or `EXECUTE`

## Key Classes

- `PermissionUtils` -- static utility class
  - `extractPermissionData(String permissionName)` -- parses a permission string
  - `hasRequiredFields(PermissionData)` / `hasNoRequiredFields(PermissionData)` -- validation helpers
- `PermissionData` -- value object with `resource`, `type`, `action`, `permissionName`
- `PermissionType` -- enum: `DATA`, `SETTINGS`, `PROCEDURAL`
- `PermissionAction` -- enum: `VIEW`, `CREATE`, `EDIT`, `DELETE`, `MANAGE`, `EXECUTE`

## Example

```java
var data = PermissionUtils.extractPermissionData("foo.items.item.post");
// data.getResource()  -> "Foo Items Item"
// data.getType()      -> DATA
// data.getAction()    -> CREATE
```

## Usage

```xml
<dependency>
  <groupId>org.folio</groupId>
  <artifactId>folio-permission-utils</artifactId>
  <version>${applications-poc-tools.version}</version>
</dependency>
```
