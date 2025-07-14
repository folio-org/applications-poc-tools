package org.folio.common.utils.permission;

import static java.util.Collections.emptySet;
import static java.util.Map.entry;
import static org.apache.commons.lang3.ObjectUtils.allNotNull;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.split;
import static org.folio.common.utils.permission.model.PermissionAction.CREATE;
import static org.folio.common.utils.permission.model.PermissionAction.DELETE;
import static org.folio.common.utils.permission.model.PermissionAction.EDIT;
import static org.folio.common.utils.permission.model.PermissionAction.EXECUTE;
import static org.folio.common.utils.permission.model.PermissionAction.MANAGE;
import static org.folio.common.utils.permission.model.PermissionAction.VIEW;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.folio.common.utils.permission.model.PermissionAction;
import org.folio.common.utils.permission.model.PermissionData;
import org.folio.common.utils.permission.model.PermissionType;

@UtilityClass
public class PermissionUtils {

  private static final Map<PermissionAction, Collection<String>> DATA_SETTINGS_ACTIONS = Map.ofEntries(
    entry(VIEW, List.of("get", "view", "read", "get-all", "read-all", "search")),
    entry(CREATE, List.of("post", "create", "write")),
    entry(EDIT, List.of("put", "edit", "update", "patch")),
    entry(DELETE, List.of("delete", "delete-all")),
    entry(MANAGE, List.of("all", "manage", "allops")));

  private static final List<String> DATA_KEYWORD_IDENTIFIERS = List.of("item", "collection", "items");
  private static final Set<String> DATA_SUFFIXES = Set.of(".item.post", ".collection.post");
  private static final List<String> SETTINGS_KEYWORDS = List.of("module", "settings");
  private static final Collection<String> PROCEDURAL_KEYWORDS = Set.of(
    "post", "download", "export", "assign", "restore", "approve", "reopen", "start", "unopen", "validate",
    "resend", "run-jobs", "stop-jobs", "generate", "reset", "test", "import", "cancel", "exportCSV",
    "showHidden", "updateEncumbrances", "execute", "move");

  public static PermissionData extractPermissionData(String permissionName) {
    var permissionParts = split(permissionName, ".");
    var permissionType = calculatePermissionType(permissionName, permissionParts);
    var resourceActionEntry = calculatePermissionResourceAndAction(permissionParts, permissionType);
    var resource = resourceActionEntry.getKey();
    var action = resourceActionEntry.getValue();

    return new PermissionData(resource, permissionType, action, permissionName);
  }

  public static boolean hasRequiredFields(PermissionData permissiondata) {
    return allNotNull(permissiondata.getType(), permissiondata.getResource(), permissiondata.getAction());
  }

  public static boolean hasNoRequiredFields(PermissionData permissionData) {
    return !hasRequiredFields(permissionData);
  }

  private static PermissionType calculatePermissionType(String name, String[] permissionParts) {
    if (containsAny(permissionParts, SETTINGS_KEYWORDS) || startsWithAny(name, SETTINGS_KEYWORDS)) {
      return PermissionType.SETTINGS;
    }

    var containsDataKeywords = containsAny(permissionParts, DATA_KEYWORD_IDENTIFIERS);
    if (endWithAny(name, PROCEDURAL_KEYWORDS) && !containsDataKeywords) {
      return PermissionType.PROCEDURAL;
    }

    if (endWithAny(name, DATA_SUFFIXES)) {
      return PermissionType.DATA;
    }

    if (containsAny(permissionParts, PROCEDURAL_KEYWORDS)) {
      return PermissionType.PROCEDURAL;
    }

    return PermissionType.DATA;
  }

  private static Entry<String, PermissionAction> calculatePermissionResourceAndAction(String[] parts,
    PermissionType type) {
    var length = parts.length;
    if (length <= 1) {
      return new SimpleImmutableEntry<>(null, null);
    }

    var action = getActionByString(parts[length - 1], type);
    if (type == PermissionType.PROCEDURAL) {
      var endIdx = action != null ? length - 2 : length - 1;
      return new SimpleImmutableEntry<>(getResourceName(parts, endIdx), EXECUTE);
    }

    if (action == null && type == PermissionType.SETTINGS) {
      return new SimpleImmutableEntry<>(getResourceName(parts, length - 1), VIEW);
    }

    return new SimpleImmutableEntry<>(getResourceName(parts, length - 2), action);
  }

  private static String getResourceName(String[] parts, int endIdx) {
    var resourceNameBuilder = new StringJoiner(" ");
    for (int i = 0; i <= Math.min(parts.length - 1, endIdx); i++) {
      var nameParts = split(parts[i], "_");
      for (var namePart : nameParts) {
        resourceNameBuilder.add(toUpperKebabCase(namePart));
      }
    }

    var resultString = resourceNameBuilder.toString();
    return resultString.startsWith("Ui") ? "UI" + resultString.substring(2) : resultString;
  }

  private static PermissionAction getActionByString(String action, PermissionType type) {
    return switch (type) {
      case SETTINGS, DATA -> getByMatchingValues(action, v -> DATA_SETTINGS_ACTIONS.getOrDefault(v, emptySet()));
      case PROCEDURAL -> getByMatchingValues(action, v -> v == EXECUTE ? PROCEDURAL_KEYWORDS : emptySet());
    };
  }

  private static PermissionAction getByMatchingValues(String action,
    Function<PermissionAction, Collection<String>> keywordProvider) {
    for (var permAction : PermissionAction.values()) {
      if (keywordProvider.apply(permAction).contains(action)) {
        return permAction;
      }
    }

    return null;
  }

  private static boolean containsAny(Object[] array, Collection<?> values) {
    for (var arrayValue : array) {
      if (values.contains(arrayValue)) {
        return true;
      }
    }

    return false;
  }

  private static boolean startsWithAny(String string, Collection<String> prefixes) {
    for (var prefix : prefixes) {
      if (string.startsWith(prefix)) {
        return true;
      }
    }

    return false;
  }

  private static boolean endWithAny(String string, Collection<String> prefixes) {
    for (var prefix : prefixes) {
      if (string.endsWith(prefix)) {
        return true;
      }
    }

    return false;
  }

  private static String toUpperKebabCase(String kebabCaseString) {
    if (!kebabCaseString.contains("-")) {
      return capitalize(kebabCaseString);
    }

    var result = new StringJoiner("-");
    for (String s : split(kebabCaseString, "-")) {
      result.add(capitalize(s));
    }

    return result.toString();
  }
}
