package org.folio.security.integration.keycloak.service;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.folio.common.utils.UuidUtils.randomId;

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
import org.folio.security.integration.keycloak.client.KeycloakAdminClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.integration.keycloak.model.KeycloakMappings;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.HttpClientErrorException;

@Log4j2
@RequiredArgsConstructor
public class KeycloakImportService {

  public static final String DESCRIPTOR_HASH_ATTR = "descriptor_hash";
  public static final String CLIENT_ID_ATTR = "client_id";
  public static final String REALM = "master";
  public static final String CLIENT_AUTH_TYPE = "client-secret";

  private final KeycloakAdminClient kc;
  private final KeycloakProperties props;
  private final InternalModuleDescriptorProvider descriptorProvider;
  private final KeycloakModuleDescriptorMapper mapper;

  @EventListener(ApplicationReadyEvent.class)
  public void importData() {
    var descriptor = descriptorProvider.getModuleDescriptor();
    var descriptorHash = descriptor.hashCode();
    var mappings = mapper.map(descriptor, false);
    var clientId = props.getClient().getClientId();
    var client = getClientIfExists(clientId);
    if (client != null) {
      if (isClientOutdated(client, descriptorHash)) {
        log.info("Existing client '{}' is outdated. Removing...", clientId);
        removeClient(client);
        removeOutdatedClientRoles(clientId, mappings);
      } else {
        log.info("Client '{}' already exist. Skipping import...", clientId);
        assignRolesToAdminClient(mappings);
        return;
      }
    }
    client = prepareClient(mappings, descriptorHash);
    createRecords(client, mappings);
    assignRolesToAdminClient(mappings);
  }

  private void assignRolesToAdminClient(KeycloakMappings mappings) {
    var adminClientId = props.getAdmin().getClientId();
    var adminClientRep = getClientIfExists(adminClientId);
    if (adminClientRep == null) {
      throw new RuntimeException("Admin client is not found by clientId: " + adminClientId);
    }

    findServiceAccountUserId(adminClientRep).ifPresent(serviceAccountUserId -> {
      log.info("Assigning mapped roles to service account user of admin client: {}", adminClientId);
      var availableRoles = kc.listAvailableRealmRoleMappings(REALM, serviceAccountUserId);
      var mappedRoles = mappings.getRoles();
      var rolesToAssign = emptyIfNull(availableRoles).stream()
        .filter(availableRole -> mappedRoles.stream()
          .anyMatch(mappedRole -> Objects.equals(availableRole.getName(), mappedRole.getName())))
        .collect(Collectors.toList());

      kc.addRealmRoleMappings(REALM, serviceAccountUserId, rolesToAssign);
    });
  }

  private void createRecords(ClientRepresentation client, KeycloakMappings mappings) {
    log.info("Importing data to Keycloak");
    var clientUuid = client.getId();

    createClient(client);
    createRoles(client, mappings.getRoles());
    createRolePolicies(clientUuid, mappings.getRolePolicies());
    createPermissions(clientUuid, mappings.getScopePermissions());

    log.info("Finished importing");
  }

  private void createClient(ClientRepresentation client) {
    log.info("Creating client '{}'", client.getClientId());
    kc.createClient(REALM, client);
  }

  private void createRoles(ClientRepresentation client, Collection<RoleRepresentation> roles) {
    roles.forEach(role -> {
      role.singleAttribute(CLIENT_ID_ATTR, client.getClientId());
      createRoleIgnoringConflict(role);
    });
  }

  private void createRoleIgnoringConflict(RoleRepresentation role) {
    try {
      log.info("Creating role '{}'", role.getName());
      kc.createRole(REALM, role);
    } catch (HttpClientErrorException.Conflict e) {
      log.info("Role '{}' already exist", role.getName());
    }
  }

  private void createRolePolicies(String clientUuid, Collection<RolePolicyRepresentation> rolePolicies) {
    rolePolicies.forEach(policy -> {
      log.info("Creating policy '{}'", policy.getName());
      kc.createRolePolicy(REALM, clientUuid, policy);
    });
  }

  private void createPermissions(String clientUuid, Collection<ScopePermissionRepresentation> permissions) {
    permissions.forEach(permission -> {
      log.info("Creating permission '{}'", permission.getName());
      kc.createScopePermission(REALM, clientUuid, permission);
    });
  }

  private void removeClient(ClientRepresentation client) {
    kc.deleteClient(REALM, client.getId());
  }

  private void removeOutdatedClientRoles(String clientId, KeycloakMappings mappings) {
    var existingRoles = emptyIfNull(kc.listRoles(REALM, false));
    var mappedRoles = mappings.getRoles();

    existingRoles.stream().filter(isOutdatedClientRole(clientId, mappedRoles)).forEach(existingRole -> {
      log.info("Removing outdated role '{}'", existingRole.getName());
      kc.deleteRoleById(REALM, existingRole.getId());
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

  private ClientRepresentation getClientIfExists(String clientId) {
    var resultList = kc.findClientsByClientId(REALM, clientId);
    return CollectionUtils.isNotEmpty(resultList) ? resultList.get(0) : null;
  }

  private static boolean isClientOutdated(ClientRepresentation client, int descriptorHash) {
    var existingHash = NumberUtils.toInt(client.getAttributes().get(DESCRIPTOR_HASH_ATTR));
    return existingHash != descriptorHash;
  }

  private Optional<String> findServiceAccountUserId(ClientRepresentation client) {
    try {
      var user = kc.getServiceAccountUser(REALM, client.getId());
      return Optional.ofNullable(user).map(UserRepresentation::getId);
    } catch (Exception e) {
      log.warn("Failed to find service account user for client with clientId: {}", client.getClientId(), e);
      return Optional.empty();
    }
  }
}
