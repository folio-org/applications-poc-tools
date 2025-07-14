package org.folio.common.utils.permission;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.folio.common.utils.permission.PermissionUtils.extractPermissionData;
import static org.folio.common.utils.permission.PermissionUtils.hasNoRequiredFields;
import static org.folio.common.utils.permission.PermissionUtils.hasRequiredFields;

import org.folio.common.utils.permission.model.PermissionAction;
import org.folio.common.utils.permission.model.PermissionData;
import org.folio.common.utils.permission.model.PermissionType;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@UnitTest
class PermissionUtilsTest {

  @ParameterizedTest(name = "[{index}] permission = {0}")
  @CsvSource(nullValues = {"null"}, value = {
    // matched data permissions
    "test-resource.view, Test-Resource, DATA, VIEW",
    "search.index.indices.item.post, Search Index Indices Item, DATA, CREATE",
    "browse_subjects_instances_collection.view, Browse Subjects Instances Collection, DATA, VIEW",
    "inventory-storage.items.collection.get, Inventory-Storage Items Collection, DATA, VIEW",
    "inventory-storage.items.collection.delete, Inventory-Storage Items Collection, DATA, DELETE",
    "inventory-storage.items.item.get, Inventory-Storage Items Item, DATA, VIEW",
    "orders.item.post, Orders Item, DATA, CREATE",
    "notes_domain.manage, Notes Domain, DATA, MANAGE",
    "perms.permissions.get, Perms Permissions, DATA, VIEW",
    "feefines_item.get, Feefines Item, DATA, VIEW",
    "feefines_item.view, Feefines Item, DATA, VIEW",
    "feefines_item.edit, Feefines Item, DATA, EDIT",
    "feefines_item.delete, Feefines Item, DATA, DELETE",
    "owners_collection.view, Owners Collection, DATA, VIEW",
    "perms.all, Perms, DATA, MANAGE",
    "ui-plugin-find-agreement.search, UI-Plugin-Find-Agreement, DATA, VIEW",
    "roles.collection.post, Roles Collection, DATA, CREATE",

    //matched procedural permissions
    "search_index_inventory_reindex.execute, Search Index Inventory Reindex, PROCEDURAL, EXECUTE",
    "search_index_records_collection.execute, Search Index Records Collection, PROCEDURAL, EXECUTE",
    "accounts_check-waive.execute, Accounts Check-Waive, PROCEDURAL, EXECUTE",
    "data-export.job.item.execute, Data-Export Job Item, PROCEDURAL, EXECUTE",
    "circulation.end-patron-action-session.post, Circulation End-Patron-Action-Session, PROCEDURAL, EXECUTE",
    "ui-inventory.item.move, UI-Inventory Item, PROCEDURAL, EXECUTE",

    // matched settings permissions
    "ui-acquisition-units.settings.all, UI-Acquisition-Units Settings, SETTINGS, MANAGE",
    "settings_ldp_enabled.view, Settings Ldp Enabled, SETTINGS, VIEW",
    "module.circulation-log.enabled, Module Circulation-Log Enabled, SETTINGS, VIEW",
    "module_erm-comparisons_enabled.view, Module Erm-Comparisons Enabled, SETTINGS, VIEW",
    "settings_erm-comparisons_enabled.view, Settings Erm-Comparisons Enabled, SETTINGS, VIEW",
    "ui-circulation.settings.loan-history, UI-Circulation Settings Loan-History, SETTINGS, VIEW",
    "ui.settings.example, UI Settings Example, SETTINGS, VIEW",

    // unmatched values
    "unknown.unknown, Unknown, DATA, null",
    "permission, null, DATA, null",
  })
  void extractPermissionData_parameterized(String permissionName, String expectedResource, PermissionType expectedType,
    PermissionAction expectedAction) {
    var permissionData = extractPermissionData(permissionName);

    var expected = data(expectedResource, expectedType, expectedAction, permissionName);
    assertThat(permissionData).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource(nullValues = {"null"}, value = {
    "Test-Resource, DATA, VIEW, true",
    "Test-Resource, DATA, null, false",
    "Test-Resource, null, VIEW, false",
    "null, DATA, VIEW, false",
  })
  void hasRequiredFields_parameterized(String resource, PermissionType type, PermissionAction action,
    boolean expected) {
    var permissionData = data(resource, type, action);

    var result = hasRequiredFields(permissionData);
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource(nullValues = {"null"}, value = {
    "Test-Resource, DATA, VIEW, false",
    "Test-Resource, DATA, null, true",
    "Test-Resource, null, VIEW, true",
    "null, DATA, VIEW, true",
  })
  void hasNoRequiredFields_parameterized(String resource, PermissionType type, PermissionAction action,
    boolean expected) {
    var permissionData = data(resource, type, action);

    var result = hasNoRequiredFields(permissionData);
    assertThat(result).isEqualTo(expected);
  }

  static PermissionData data(String resource, PermissionType type, PermissionAction action, String permissionName) {
    return new PermissionData(resource, type, action, permissionName);
  }

  static PermissionData data(String resource, PermissionType type, PermissionAction action) {
    return new PermissionData(resource, type, action, null);
  }
}
