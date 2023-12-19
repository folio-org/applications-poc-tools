package org.folio.security.integration.keycloak.service;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.folio.common.utils.CollectionUtils.toStream;
import static org.folio.common.utils.UuidUtils.randomId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.security.domain.model.descriptor.ModuleDescriptor;
import org.folio.security.domain.model.descriptor.Permission;
import org.folio.security.domain.model.descriptor.RoutingEntry;
import org.folio.security.integration.keycloak.model.KeycloakMappings;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.springframework.http.HttpMethod;

@Log4j2
public class KeycloakModuleDescriptorMapper {

  public static final List<String> HTTP_METHODS =
    Arrays.stream(HttpMethod.values()).map(HttpMethod::name).collect(toList());
  public static final String ROLE_POLICY_PREFIX = "Policy for role: ";
  private static final String PERM_ATTRIBUTE_KEY = "folio_permissions";
  private static final String SCOPE_PERM_DELIMITER = "#";

  public KeycloakMappings map(ModuleDescriptor descriptor) {
    log.debug("Preparing keycloak mappings from module descriptor [id='{}']", descriptor.getId());

    var allScopes = mapScopes(HTTP_METHODS);
    var roleMappings = mapRoles(descriptor);
    var rolePolicyMappings = mapPolicies(roleMappings);
    var resourceMappings = mapResources(descriptor);
    var resourceScopeMappings = associateScopes(allScopes, resourceMappings);
    var permissions = mapPermissions(rolePolicyMappings, resourceScopeMappings, resourceMappings);
    var resourceServer = mapResourceServer(allScopes, resourceMappings.values());

    return KeycloakMappings.builder()
      .roles(roleMappings.values())
      .rolePolicies(rolePolicyMappings.values())
      .scopePermissions(permissions)
      .resourceServer(resourceServer)
      .build();
  }

  public Map<Permission, RoleRepresentation> mapRoles(ModuleDescriptor descriptor) {
    var roles = new HashMap<Permission, RoleRepresentation>();
    for (var permissionSet : getPermissionSets(descriptor)) {
      if (BooleanUtils.isTrue(permissionSet.getVisible())) {
        var role = mapRole(permissionSet);
        roles.put(permissionSet, role);
      }
    }
    return roles;
  }

  public Map<Permission, RolePolicyRepresentation> mapPolicies(Map<Permission, RoleRepresentation> roles) {
    var policies = new HashMap<Permission, RolePolicyRepresentation>();
    for (var entry : roles.entrySet()) {
      var policy = mapRolePolicy(entry.getValue());
      policies.put(entry.getKey(), policy);
    }
    return policies;
  }

  public Map<RoutingEntry, ResourceRepresentation> mapResources(ModuleDescriptor descriptor) {
    var resourceMap = new HashMap<String, ResourceRepresentation>();
    var routingEntryResourceMap = new HashMap<RoutingEntry, ResourceRepresentation>();
    for (var handler : getRoutingEntries(descriptor)) {
      var path = StringUtils.getIfEmpty(handler.getPath(), handler::getPathPattern);

      // one resource can represent multiple routing entries
      var resource = resourceMap.computeIfAbsent(path, this::mapResource);
      var resourceAttribute = prepareAttributeForResources(handler);

      // combine permissions for resources
      setAttributesForResource(resource, resourceAttribute);
      // ad-hoc to support "resource by permission" lookup in scope of US1118978
      // (keycloak doesn't support search for resources by attribute)
      updateResourceType(resource, resourceAttribute);

      routingEntryResourceMap.put(handler, resource);
    }

    return routingEntryResourceMap;
  }

  public List<ScopeRepresentation> mapScopes(List<String> methods) {
    var scopes = new ArrayList<ScopeRepresentation>();
    for (var method : methods) {
      var scope = new ScopeRepresentation(method);
      scope.setId(randomId());
      scopes.add(scope);
    }
    return scopes;
  }

  public static ScopePermissionRepresentation mapPermission(String permissionName) {
    var permission = new ScopePermissionRepresentation();
    permission.setId(randomId());
    permission.setName(permissionName);
    return permission;
  }

  private static String prepareAttributeForResources(RoutingEntry handler) {
    if (Objects.isNull(handler)) {
      return EMPTY;
    }
    var methods = emptyIfNull(handler.getMethods());
    var permissionsWithScopes = emptyIfNull(handler.getPermissionsRequired())
      .stream().map(perm -> String.join(SCOPE_PERM_DELIMITER, methods) + SCOPE_PERM_DELIMITER + perm)
      .collect(toSet());
    return String.join(",", permissionsWithScopes);
  }

  private static void setAttributesForResource(ResourceRepresentation resource, String permissions) {
    if (MapUtils.isEmpty(resource.getAttributes())) {
      var mapAttr = new HashMap<String, List<String>>();
      mapAttr.put(PERM_ATTRIBUTE_KEY, List.of(permissions));
      resource.setAttributes(mapAttr);
    } else {
      var mapAttr = resource.getAttributes();
      var attr = mapAttr.get(PERM_ATTRIBUTE_KEY);
      var attrToSet = CollectionUtils.isEmpty(attr)
        ? List.of(permissions)
        : List.of(String.join(",", attr.get(0), permissions));
      mapAttr.put(PERM_ATTRIBUTE_KEY, attrToSet);
      resource.setAttributes(mapAttr);
    }
  }

  private static void updateResourceType(ResourceRepresentation resource, String permissions) {
    var type = resource.getType();

    if (isEmpty(type)) {
      resource.setType(permissions);
    } else {
      resource.setType(String.join(",", type, permissions));
    }
  }

  private static RoleRepresentation mapRole(Permission permissionSet) {
    var role = new RoleRepresentation();
    role.setId(randomId());
    role.setName(permissionSet.getDisplayName());
    role.setDescription(permissionSet.getDescription());
    return role;
  }

  private static RolePolicyRepresentation mapRolePolicy(RoleRepresentation role) {
    var policy = new RolePolicyRepresentation();
    policy.setId(randomId());
    policy.setName(ROLE_POLICY_PREFIX + role.getName());
    policy.addRole(role.getName());
    return policy;
  }

  private ResourceRepresentation mapResource(String path) {
    var resource = new ResourceRepresentation();
    resource.setId(randomId());
    resource.setName(path);
    return resource;
  }

  private static ResourceServerRepresentation mapResourceServer(List<ScopeRepresentation> scopes,
    Collection<ResourceRepresentation> resources) {
    var authorizationSettings = new ResourceServerRepresentation();
    authorizationSettings.setScopes(scopes);
    authorizationSettings.setResources(new ArrayList<>(resources));
    authorizationSettings.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
    return authorizationSettings;
  }

  private static Map<RoutingEntry, Set<ScopeRepresentation>> associateScopes(
    List<ScopeRepresentation> allScopes,
    Map<RoutingEntry, ResourceRepresentation> resourceMappings) {
    var allResourceScopes = new HashMap<RoutingEntry, Set<ScopeRepresentation>>();
    for (var entry : resourceMappings.entrySet()) {
      var handler = entry.getKey();
      var resource = entry.getValue();

      var methods = handler.getMethods();
      var resourceScopes = findScopes(allScopes, methods);
      resourceScopes.forEach(scope -> resource.addScope(scope.getName()));

      allResourceScopes.put(handler, resourceScopes);
    }
    return allResourceScopes;
  }

  private static List<ScopePermissionRepresentation> mapPermissions(
    Map<Permission, RolePolicyRepresentation> rolePolicyMappings,
    Map<RoutingEntry, Set<ScopeRepresentation>> scopeMappings,
    Map<RoutingEntry, ResourceRepresentation> resourceMappings) {

    var allPermissions = new ArrayList<ScopePermissionRepresentation>();
    for (var entry : resourceMappings.entrySet()) {
      var handler = entry.getKey();
      var resource = entry.getValue();

      var resourceScopes = scopeMappings.get(handler);

      var requiredPermissions = emptyIfNull(handler.getPermissionsRequired());
      var associatedPolicies = findPermissionPolicies(rolePolicyMappings, requiredPermissions);

      var resourcePermissions = mapAndAssociateResourcePermissions(resource, resourceScopes, associatedPolicies);

      allPermissions.addAll(resourcePermissions);
    }
    return allPermissions;
  }

  private static List<ScopePermissionRepresentation> mapAndAssociateResourcePermissions(
    ResourceRepresentation resource,
    Set<ScopeRepresentation> scopes, List<RolePolicyRepresentation> associatedPolicies) {
    var permissions = new ArrayList<ScopePermissionRepresentation>(associatedPolicies.size());
    for (var policy : associatedPolicies) {
      var scopeNames = scopes.stream().map(ScopeRepresentation::getName).collect(toSet());
      var permissionName = String.format("%s access for role '%s' to '%s'",
        scopeNames, policy.getName().substring(ROLE_POLICY_PREFIX.length()), resource.getName());

      var permission = mapPermission(permissionName);
      permission.addResource(resource.getName());
      permission.addPolicy(policy.getName());
      scopes.forEach(scope -> permission.addScope(scope.getName()));

      permissions.add(permission);
    }

    return permissions;
  }

  private static List<Permission> findParentPermissionsOf(Collection<Permission> permissionSets,
    String childPermission) {
    var parentPermissions = new ArrayList<Permission>();
    for (var permissionSet : permissionSets) {
      if (!BooleanUtils.isTrue(permissionSet.getVisible())) {
        continue;
      }

      var subPermissions = emptyIfNull(permissionSet.getSubPermissions());

      if (subPermissions.contains(childPermission)) {
        parentPermissions.add(permissionSet);

        var subList = CollectionUtils.removeAll(permissionSets, List.of(permissionSet));
        var parents = findParentPermissionsOf(subList, permissionSet.getPermissionName());
        parentPermissions.addAll(parents);
      }
    }
    return parentPermissions;
  }

  /**
   * Finds role policies representing permission sets that are parents of the given permissions in module descriptor.
   *
   * @param policyMappings permission set to policy mappings
   * @param permissions required permissions
   * @return role policies associated with the given scope permissions
   */
  private static List<RolePolicyRepresentation> findPermissionPolicies(
    Map<Permission, RolePolicyRepresentation> policyMappings,
    List<String> permissions) {

    var permissionSets = policyMappings.keySet();
    var parentPermissionSets = permissions.stream()
      .flatMap(permission -> findParentPermissionsOf(permissionSets, permission).stream())
      .collect(toList());

    var associatedPolicies = new ArrayList<RolePolicyRepresentation>();
    for (var permissionSet : parentPermissionSets) {
      var policy = policyMappings.get(permissionSet);
      associatedPolicies.add(policy);
    }
    return associatedPolicies;
  }

  private static Set<ScopeRepresentation> findScopes(List<ScopeRepresentation> allScopes, List<String> scopeName) {
    return scopeName.stream().map(name -> filterScope(allScopes, name)).flatMap(Optional::stream).collect(toSet());
  }

  private static Optional<ScopeRepresentation> filterScope(Collection<ScopeRepresentation> scopes, String name) {
    return scopes.stream().filter(scope -> scope.getName().equals(name)).findFirst();
  }

  private static List<RoutingEntry> getRoutingEntries(ModuleDescriptor descriptor) {
    return toStream(descriptor.getProvides())
      .flatMap(i -> toStream(i.getHandlers()))
      .collect(toList());
  }

  private static List<Permission> getPermissionSets(ModuleDescriptor descriptor) {
    return emptyIfNull(descriptor.getPermissionSets());
  }
}
