package org.folio.security.integration.keycloak.model;

import java.util.Collection;
import lombok.Builder;
import lombok.Data;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.representations.idm.authorization.RolePolicyRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;

@Data
@Builder
public class KeycloakMappings {

  private Collection<RoleRepresentation> roles;
  private Collection<RolePolicyRepresentation> rolePolicies;
  private Collection<ScopePermissionRepresentation> scopePermissions;
  private ResourceServerRepresentation resourceServer;
}
