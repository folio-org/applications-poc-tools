# folio-permission-utils

A lightweight, zero-framework Java library for parsing FOLIO permission name strings into
structured data. Classifies dot-separated FOLIO permission names into a type/action/resource triple
to support permission grouping, UI generation, documentation, and access-control tooling.

## Table of Contents

- [Usage](#usage)
- [Permission Types](#permission-types)
- [Permission Actions](#permission-actions)
- [Resource Name Normalization](#resource-name-normalization)
- [API Reference](#api-reference)

---

## Usage

The entire public API is three static methods on `PermissionUtils`:

```java
import org.folio.common.utils.permission.PermissionUtils;
import org.folio.common.utils.permission.model.PermissionData;

PermissionData data = PermissionUtils.extractPermissionData(
    "inventory-storage.items.collection.get");

data.getType();           // DATA
data.getAction();         // VIEW
data.getResource();       // "Inventory-Storage Items Collection"
data.getPermissionName(); // "inventory-storage.items.collection.get" (original input)
```

Check whether all fields were successfully resolved:

```java
if (PermissionUtils.hasRequiredFields(data)) {
    // type, resource, and action are all non-null
}

if (PermissionUtils.hasNoRequiredFields(data)) {
    // at least one of type, resource, or action is null
    // (e.g. for single-segment names like "ui-circulation")
}
```

> **No framework dependency.** This library has no Spring, Jakarta EE, or other runtime framework
> dependency. It works in any Java 17+ application.

---

## Permission Types

| Type         | `getValue()`   | Classification rule                                                                                                                                                               |
|:-------------|:---------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `DATA`       | `"data"`       | Permissions over CRUD data resources (items, collections, records) — the default                                                                                                  |
| `SETTINGS`   | `"settings"`   | Any segment or name start matches `"module"` or `"settings"`                                                                                                                      |
| `PROCEDURAL` | `"procedural"` | Name ends with a procedural verb (e.g. `post`, `export`, `cancel`, `import`, `execute`, `move`, ...) and does not contain data-indicator keywords (`item`, `collection`, `items`) |

---

## Permission Actions

| Action    | `getValue()` | Recognized raw keywords                                      |
|:----------|:-------------|:-------------------------------------------------------------|
| `VIEW`    | `"view"`     | `get`, `view`, `read`, `get-all`, `read-all`, `search`       |
| `CREATE`  | `"create"`   | `post`, `create`, `write`                                    |
| `EDIT`    | `"edit"`     | `put`, `edit`, `update`, `patch`                             |
| `DELETE`  | `"delete"`   | `delete`, `delete-all`                                       |
| `MANAGE`  | `"manage"`   | `all`, `manage`, `allops`                                    |
| `EXECUTE` | `"execute"`  | All procedural verbs (always used when `type == PROCEDURAL`) |

---

## Resource Name Normalization

The resource name is assembled from the non-action dot-segments of the permission name. Each
segment is title-cased, kebab-case and underscore separators are preserved, and segments are joined
with spaces:

| Input permission name                    | Resource                                 |
|:-----------------------------------------|:-----------------------------------------|
| `inventory-storage.items.collection.get` | `"Inventory-Storage Items Collection"`   |
| `circulation.loans.item.put`             | `"Circulation Loans Item"`               |
| `ui-circulation.settings.loan-history`   | `"UI-Circulation Settings Loan-History"` |
| `mod-foo.module.settings.put`            | `"Mod-Foo Module"`                       |

A leading `Ui` segment is normalized to `UI` (e.g. `ui-notes` → `"UI-Notes"`).

For single-segment names (e.g. `"orders"`) both `resource` and `action` are `null`.

---

## API Reference

### `PermissionUtils`

| Method                                         | Returns          | Description                                                 |
|:-----------------------------------------------|:-----------------|:------------------------------------------------------------|
| `extractPermissionData(String permissionName)` | `PermissionData` | Parses a FOLIO permission name into a structured triple     |
| `hasRequiredFields(PermissionData)`            | `boolean`        | `true` if `type`, `resource`, and `action` are all non-null |
| `hasNoRequiredFields(PermissionData)`          | `boolean`        | `true` if any of `type`, `resource`, or `action` is null    |

### `PermissionData`

| Field            | Type               | Description                                                |
|:-----------------|:-------------------|:-----------------------------------------------------------|
| `permissionName` | `String`           | The original raw permission string                         |
| `type`           | `PermissionType`   | `DATA`, `SETTINGS`, or `PROCEDURAL`                        |
| `action`         | `PermissionAction` | `VIEW`, `CREATE`, `EDIT`, `DELETE`, `MANAGE`, or `EXECUTE` |
| `resource`       | `String`           | Human-readable, title-cased resource name                  |

### `PermissionType`

`DATA`, `SETTINGS`, `PROCEDURAL` — each provides `getValue()` returning the lowercase string value.
Use `PermissionType.fromValue(String)` for reverse lookup (throws `IllegalArgumentException` on
unknown values).

### `PermissionAction`

`VIEW`, `CREATE`, `EDIT`, `DELETE`, `MANAGE`, `EXECUTE` — each provides `getValue()` returning the
lowercase string value. Use `PermissionAction.fromValue(String)` for reverse lookup.
