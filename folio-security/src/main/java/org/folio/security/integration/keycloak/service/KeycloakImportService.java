package org.folio.security.integration.keycloak.service;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.folio.common.utils.UuidUtils.randomId;

import jakarta.ws.rs.ClientErrorException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.integration.keycloak.model.KeycloakMappings;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Log4j2
@RequiredArgsConstructor
public class KeycloakImportService {

  public static final String DESCRIPTOR_HASH_ATTR = "descriptor_hash";
  public static final String CLIENT_ID_ATTR = "client_id";
  public static final String REALM = "master";
  public static final String CLIENT_AUTH_TYPE = "client-secret";

  private final Keycloak keycloakClient;
  private final KeycloakProperties props;
  private final InternalModuleDescriptorProvider descriptorProvider;
  private final KeycloakModuleDescriptorMapper mapper;

  @EventListener(ApplicationReadyEvent.class)
  public void importData() {
    var descriptor = descriptorProvider.getModuleDescriptor();
    var descriptorHash = descriptor.hashCode();
    var mappings = mapper.map(descriptor);
    var clientId = props.getClient().getClientId();
    var realmResource = keycloakClient.realm(REALM);
    var client = getClientIfExists(realmResource, clientId);
    if (client != null) {
      if (isClientOutdated(client, descriptorHash)) {
        log.info("Existing client '{}' is outdated. Removing...", clientId);
        removeClient(realmResource, client);
        removeOutdatedClientRoles(realmResource, clientId, mappings);
      } else {
        log.info("Client '{}' already exist. Skipping import...", clientId);
        assignRolesToAdminClient(realmResource, mappings);
        return;
      }
    }
    client = prepareClient(mappings, descriptorHash);
    createRecords(realmResource, client, mappings);
    assignRolesToAdminClient(realmResource, mappings);
  }

  private void assignRolesToAdminClient(RealmResource realm, KeycloakMappings mappings) {
    var adminClientId = props.getAdmin().getClientId();
    var adminClient = getClientIfExists(realm, adminClientId);
    if (adminClient == null) {
      throw new RuntimeException("Admin client is not found by clientId: " + adminClientId);
    }

    findServiceAccountUserId(adminClient).ifPresent(serviceAccountUserId -> {
      log.info("Assigning mapped roles to service account user of admin client: {}", adminClientId);
      var roleMappingsResource = roleMappingResource(serviceAccountUserId);
      var availableRoles = roleMappingsResource.realmLevel().listAvailable();
      var mappedRoles = mappings.getRoles();
      var rolesToAssign = availableRoles.stream()
        .filter(availableRole -> mappedRoles.stream()
          .anyMatch(mappedRole -> Objects.equals(availableRole.getName(), mappedRole.getName())))
        .collect(Collectors.toList());

      roleMappingsResource.realmLevel().add(rolesToAssign);
    });
  }

  private void createRecords(RealmResource realmResource, ClientRepresentation client, KeycloakMappings mappings) {
    log.info("Importing data to Keycloak");
    var authResource = getAuthorizationResource(client);

    createClient(realmResource, client);

    createRoles(realmResource, client, mappings.getRoles());

    createRolePolicies(authResource, mappings.getRolePolicies());

    createPermissions(authResource, mappings.getScopePermissions());

    log.info("Finished importing");
  }

  private static void createClient(RealmResource realm, ClientRepresentation client) {
    try (var response = realm.clients().create(client)) {
      log.info("Creating client '{}', response: {}", client.getClientId(), response.getStatus());
    }
  }

  private static void createRoles(RealmResource realm, ClientRepresentation client,
    Collection<RoleRepresentation> roles) {
    roles.forEach(role -> {
      role.singleAttribute(CLIENT_ID_ATTR, client.getClientId());
      createRoleIgnoringConflict(realm, role);
    });
  }

  private static void createRoleIgnoringConflict(RealmResource realm, RoleRepresentation role) {
    try {
      log.info("Creating role '{}'", role.getName());
      realm.roles().create(role);
    } catch (ClientErrorException exception) {
      if (exception.getResponse().getStatus() == SC_CONFLICT) {
        log.info("Role already exist");
      } else {
        throw exception;
      }
    }
  }

  private static void createRolePolicies(AuthorizationResource authResource,
    Collection<RolePolicyRepresentation> rolePolicies) {
    rolePolicies.forEach(policy -> {
      try (var resp = authResource.policies().role().create(policy)) {
        log.info("Creating policy '{}', response: {}", policy.getName(), resp.getStatus());
      }
    });
  }

  private static void createPermissions(AuthorizationResource authResource,
    Collection<ScopePermissionRepresentation> permissions) {
    permissions.forEach(permission -> {
      try (var resp = authResource.permissions().scope().create(permission)) {
        log.info("Creating permission '{}', response: {}", permission.getName(), resp.getStatus());
      }
    });
  }

  private static void removeClient(RealmResource realmResource, ClientRepresentation client) {
    realmResource.clients().get(client.getId()).remove();
  }

  private static void removeOutdatedClientRoles(RealmResource realmResource, String clientId,
    KeycloakMappings mappings) {
    var existingRoles = emptyIfNull(realmResource.roles().list(false));
    var mappedRoles = mappings.getRoles();

    existingRoles.stream().filter(isOutdatedClientRole(clientId, mappedRoles)).forEach(existingRole -> {
      log.info("Removing outdated role '{}'", existingRole.getName());
      realmResource.rolesById().deleteRole(existingRole.getId());
    });
  }

  private ClientRepresentation prepareClient(KeycloakMappings mappings, int descriptorHash) {
    var clientCredentials = props.getClient();
    var client = new ClientRepresentation();
    client.setId(randomId());

    String clientId = clientCredentials.getClientId();
    client.setClientId(clientId);
    // client secret is generated automatically by Keycloak, we don't need to memorize it.
    client.setAuthorizationSettings(mappings.getResourceServer());
    client.setClientAuthenticatorType(CLIENT_AUTH_TYPE);
    client.setDirectAccessGrantsEnabled(true);
    client.setServiceAccountsEnabled(true);
    client.setAuthorizationServicesEnabled(true);
    client.setStandardFlowEnabled(false);
    client.setPublicClient(false);
    client.setAttributes(Map.of(DESCRIPTOR_HASH_ATTR, Integer.toString(descriptorHash)));
    return client;
  }

  private static Predicate<RoleRepresentation> isOutdatedClientRole(String clientId,
    Collection<RoleRepresentation> mappedRoles) {
    return existingRole -> isClientRole(existingRole, clientId) && noneMatch(existingRole, mappedRoles);
  }

  private static boolean isClientRole(RoleRepresentation role, String clientId) {
    List<String> clientIdAttr = role.getAttributes().getOrDefault(CLIENT_ID_ATTR, Collections.emptyList());
    return clientIdAttr.contains(clientId);
  }

  private static boolean noneMatch(RoleRepresentation existing, Collection<RoleRepresentation> mappedRoles) {
    var existingName = existing.getName();
    return mappedRoles.stream().map(RoleRepresentation::getName).noneMatch(existingName::equals);
  }

  private ClientRepresentation getClientIfExists(RealmResource realm, String clientId) {
    var resultList = realm.clients().findByClientId(clientId);
    return CollectionUtils.isNotEmpty(resultList) ? resultList.get(0) : null;
  }

  private static boolean isClientOutdated(ClientRepresentation client, int descriptorHash) {
    var existingHash = NumberUtils.toInt(client.getAttributes().get(DESCRIPTOR_HASH_ATTR));
    return existingHash != descriptorHash;
  }

  private Optional<String> findServiceAccountUserId(ClientRepresentation client) {
    String userId = null;
    try {
      var userResource = keycloakClient.proxy(UserResource.class, serviceAccountUserResourceUri(client));
      userId = userResource.toRepresentation().getId();
    } catch (Exception e) {
      log.warn("Failed to find service account user for client with clientId: {}", client.getClientId(), e);
    }
    return Optional.ofNullable(userId);
  }

  private AuthorizationResource getAuthorizationResource(ClientRepresentation client) {
    return keycloakClient.proxy(AuthorizationResource.class, authorizationResourceUri(client));
  }

  private RoleMappingResource roleMappingResource(String userId) {
    return keycloakClient.proxy(RoleMappingResource.class, roleMappingsResourceUri(userId));
  }

  private URI roleMappingsResourceUri(String userId) {
    return URI.create(props.getUrl() + "/admin/realms/master/users/" + userId + "/role-mappings");
  }

  private URI authorizationResourceUri(ClientRepresentation client) {
    return URI.create(getClientResourcePath(client) + "/authz/resource-server");
  }

  private URI serviceAccountUserResourceUri(ClientRepresentation client) {
    return URI.create(getClientResourcePath(client) + "/service-account-user");
  }

  private String getClientResourcePath(ClientRepresentation client) {
    return props.getUrl() + "/admin/realms/master/clients/" + client.getId();
  }
}
