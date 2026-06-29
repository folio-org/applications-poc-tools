package org.folio.security.it;

import static org.apache.commons.lang3.SystemProperties.JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.security.integration.keycloak.service.KeycloakImportService.CLIENT_ID_ATTR;
import static org.folio.security.integration.keycloak.service.KeycloakImportService.REALM;

import org.folio.security.integration.keycloak.client.KeycloakAdminClient;
import org.folio.security.integration.keycloak.configuration.KeycloakDataImportConfiguration;
import org.folio.security.integration.keycloak.service.SecureStoreKeyProvider;
import org.folio.security.support.TestConfiguration;
import org.folio.test.extensions.EnableKeycloakDataImport;
import org.folio.test.extensions.EnableKeycloakTlsMode;
import org.folio.test.types.IntegrationTest;
import org.folio.tools.store.configuration.SecureStoreAutoconfiguration;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that the Keycloak data import (run on application startup by {@code KeycloakImportService})
 * created the expected client, roles and admin service-account role assignments in a real Keycloak
 * (Testcontainer).
 *
 * <p>Rewritten for the native-image migration to verify entirely through the Spring HTTP Interface
 * {@link KeycloakAdminClient} (plain REST), replacing the previous {@code keycloak-admin-client}
 * (RESTEasy) {@code partialExport}-based verification. Behavioural assertions are used because the REST
 * read endpoints return per-entity shapes rather than the full realm export the old JSON fixtures
 * captured.</p>
 *
 * <p>NOTE: requires a running Keycloak; it is a CI-validated integration test and cannot run on a host
 * that blocks Java NIO loopback selectors (see work log 2026-06-29).</p>
 */
@IntegrationTest
@EnableKeycloakTlsMode
@EnableKeycloakDataImport
@TestPropertySource(properties = {
  "application.secret-store.environment=folio",
})
@SpringBootTest(classes = {
  KeycloakDataImportConfiguration.class,
  SecureStoreKeyProvider.class,
  SecureStoreAutoconfiguration.class,
  TestConfiguration.class
})
class KeycloakImportServiceIT {

  private static final String CLIENT_ID = "mgr-test";
  private static final String BACKEND_ADMIN_CLIENT_ID = "folio-backend-admin-client";

  @Autowired private KeycloakAdminClient keycloakAdminClient;

  @Test
  void verifyDataImportedOnAppStartup() {
    // The application client was created.
    var clients = keycloakAdminClient.findClientsByClientId(REALM, CLIENT_ID);
    assertThat(clients).hasSize(1);
    assertThat(clients.get(0).getClientId()).isEqualTo(CLIENT_ID);

    // Realm roles tagged with the client-id attribute were created from the module descriptor.
    var createdRoleNames = keycloakAdminClient.listRoles(REALM, false).stream()
      .filter(role -> role.getAttributes() != null && role.getAttributes().containsKey(CLIENT_ID_ATTR))
      .map(RoleRepresentation::getName)
      .toList();
    assertThat(createdRoleNames).isNotEmpty();

    // The created roles were assigned to the service-account user of the admin client.
    var adminClients = keycloakAdminClient.findClientsByClientId(REALM, BACKEND_ADMIN_CLIENT_ID);
    assertThat(adminClients).hasSize(1);
    var serviceAccountUser = keycloakAdminClient.getServiceAccountUser(REALM, adminClients.get(0).getId());
    var assignedRoleNames = keycloakAdminClient
      .listAssignedRealmRoleMappings(REALM, serviceAccountUser.getId()).stream()
      .map(RoleRepresentation::getName)
      .toList();
    assertThat(assignedRoleNames).containsAnyElementsOf(createdRoleNames);
  }

  static {
    System.setProperty(JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION, "true");
  }
}
