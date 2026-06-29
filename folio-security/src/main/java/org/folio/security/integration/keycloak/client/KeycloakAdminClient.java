package org.folio.security.integration.keycloak.client;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Spring HTTP Interface client for the Keycloak Admin REST API.
 *
 * <p>Replaces the reflection- and ServiceLoader-heavy RESTEasy-based {@code keycloak-admin-client}
 * (which is hostile to GraalVM native-image) with a single JDK dynamic proxy. The request/response
 * bodies intentionally reuse the existing {@code org.keycloak.representations.*} POJOs (from
 * {@code keycloak-core}, which is retained for token validation), so Keycloak's authorization wire
 * format is not re-implemented by hand. The admin bearer token is injected by a {@code RestClient}
 * request interceptor (see {@code KeycloakDataImportConfiguration}), not threaded through each method.</p>
 */
@HttpExchange(url = "/admin/realms", contentType = APPLICATION_JSON_VALUE)
public interface KeycloakAdminClient {

  @GetExchange("/{realm}/clients")
  List<ClientRepresentation> findClientsByClientId(@PathVariable String realm,
    @RequestParam("clientId") String clientId);

  @PostExchange("/{realm}/clients")
  void createClient(@PathVariable String realm, @RequestBody ClientRepresentation client);

  @DeleteExchange("/{realm}/clients/{id}")
  void deleteClient(@PathVariable String realm, @PathVariable String id);

  @PostExchange("/{realm}/roles")
  void createRole(@PathVariable String realm, @RequestBody RoleRepresentation role);

  @GetExchange("/{realm}/roles")
  List<RoleRepresentation> listRoles(@PathVariable String realm,
    @RequestParam("briefRepresentation") boolean briefRepresentation);

  @DeleteExchange("/{realm}/roles-by-id/{id}")
  void deleteRoleById(@PathVariable String realm, @PathVariable String id);

  @PostExchange("/{realm}/clients/{clientUuid}/authz/resource-server/policy/role")
  void createRolePolicy(@PathVariable String realm, @PathVariable String clientUuid,
    @RequestBody RolePolicyRepresentation policy);

  @PostExchange("/{realm}/clients/{clientUuid}/authz/resource-server/permission/scope")
  void createScopePermission(@PathVariable String realm, @PathVariable String clientUuid,
    @RequestBody ScopePermissionRepresentation permission);

  @GetExchange("/{realm}/clients/{clientUuid}/service-account-user")
  UserRepresentation getServiceAccountUser(@PathVariable String realm, @PathVariable String clientUuid);

  @GetExchange("/{realm}/users/{userId}/role-mappings/realm/available")
  List<RoleRepresentation> listAvailableRealmRoleMappings(@PathVariable String realm,
    @PathVariable String userId);

  @GetExchange("/{realm}/users/{userId}/role-mappings/realm")
  List<RoleRepresentation> listAssignedRealmRoleMappings(@PathVariable String realm,
    @PathVariable String userId);

  @PostExchange("/{realm}/users/{userId}/role-mappings/realm")
  void addRealmRoleMappings(@PathVariable String realm, @PathVariable String userId,
    @RequestBody List<RoleRepresentation> roles);
}
