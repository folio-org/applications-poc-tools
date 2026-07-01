package org.folio.security.nativex;

import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Unconditional auto-configuration that contributes {@code folio-security}'s GraalVM native-image hints during
 * Spring AOT. It carries no beans; it exists solely so the hints register regardless of which security/import
 * conditions are active at build time (it is listed in {@code AutoConfiguration.imports}).
 *
 * <p>{@link FolioSecurityRuntimeHints} registers the Keycloak {@code @HttpExchange} client JDK proxies.
 * {@code @RegisterReflectionForBinding} covers the Jackson (de)serialized types: the Keycloak Admin REST
 * representations exchanged by {@code KeycloakAdminClient}, the token/JWK types (de)serialized by
 * {@code TokenVerifier}/{@code JWKParser}, and the local {@code TokenResponse}.</p>
 */
@AutoConfiguration
@ImportRuntimeHints(FolioSecurityRuntimeHints.class)
@RegisterReflectionForBinding({
  org.keycloak.representations.idm.ClientRepresentation.class,
  org.keycloak.representations.idm.RoleRepresentation.class,
  org.keycloak.representations.idm.UserRepresentation.class,
  org.keycloak.representations.idm.authorization.ResourceServerRepresentation.class,
  org.keycloak.representations.idm.authorization.RolePolicyRepresentation.class,
  org.keycloak.representations.idm.authorization.ScopePermissionRepresentation.class,
  org.keycloak.representations.idm.authorization.ResourceRepresentation.class,
  org.keycloak.representations.idm.authorization.ScopeRepresentation.class,
  org.keycloak.representations.AccessToken.class,
  org.keycloak.jose.jwk.JSONWebKeySet.class,
  org.folio.security.integration.keycloak.model.TokenResponse.class
})
public class FolioSecurityNativeHintsAutoConfiguration {
}
