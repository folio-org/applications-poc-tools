package org.folio.test.extensions;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.extensions.impl.KeycloakContainerExtension;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RoleRepresentation;

@UnitTest
@EnableKeycloakTlsMode
class EnableKeycloakTlsModeTest {

  private static final String MASTER_REALM = "master";
  private static final String FOLIO_BACKEND_ADMIN_CLIENT = "folio-backend-admin-client";

  @Test
  void keycloakContainer_positive() {
    assertThat(System.getProperty("KC_URL")).isNotBlank();

    var adminClient = KeycloakContainerExtension.getKeycloakAdminClient();
    var masterRealm = adminClient.realm(MASTER_REALM).toRepresentation();

    assertThat(masterRealm.getRealm()).isEqualTo(MASTER_REALM);
    assertThat(masterRealm.getAccessTokenLifespan()).isEqualTo(900);
  }

  @Test
  void systemProperties_positive_allSet() {
    assertThat(System.getProperty("KC_URL")).startsWith("https://");
    assertThat(System.getProperty("KC_ADMIN_CLIENT_ID")).isEqualTo(FOLIO_BACKEND_ADMIN_CLIENT);
    assertThat(System.getProperty("KC_ADMIN_CLIENT_SECRET")).isNotBlank();
    assertThat(System.getProperty("KC_ADMIN_USERNAME")).isNotBlank();
    assertThat(System.getProperty("KC_ADMIN_PASSWORD")).isNotBlank();
    assertThat(System.getProperty("KC_ADMIN_GRANT_TYPE")).isEqualTo("client_credentials");
  }

  @Test
  void folioBackendAdminClient_positive_clientPresent() {
    var adminClient = KeycloakContainerExtension.getKeycloakAdminClient();
    var clients = adminClient.realm(MASTER_REALM).clients().findByClientId(FOLIO_BACKEND_ADMIN_CLIENT);

    assertThat(clients).hasSize(1);
    assertThat(clients.getFirst().isServiceAccountsEnabled()).isTrue();
  }

  @Test
  void folioBackendAdminClientRoles_positive_adminAndCreateRealmRolesAssigned() {
    var adminClient = KeycloakContainerExtension.getKeycloakAdminClient();
    var clients = adminClient.realm(MASTER_REALM).clients().findByClientId(FOLIO_BACKEND_ADMIN_CLIENT);

    var clientUuid = clients.getFirst().getId();
    var serviceAccountUser = adminClient.realm(MASTER_REALM).clients().get(clientUuid).getServiceAccountUser();
    var roleNames = adminClient.realm(MASTER_REALM).users().get(serviceAccountUser.getId())
      .roles().realmLevel().listAll().stream()
      .map(RoleRepresentation::getName)
      .toList();

    assertThat(roleNames).contains("admin", "create-realm");
  }
}
