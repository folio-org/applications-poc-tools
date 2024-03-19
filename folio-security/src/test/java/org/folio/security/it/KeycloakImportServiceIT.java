package org.folio.security.it;

import static org.folio.security.integration.keycloak.service.KeycloakImportService.CLIENT_ID_ATTR;
import static org.folio.security.integration.keycloak.service.KeycloakImportService.REALM;
import static org.folio.test.TestUtils.OBJECT_MAPPER;
import static org.folio.test.TestUtils.readString;
import static org.skyscreamer.jsonassert.JSONCompareMode.LENIENT;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.security.integration.keycloak.configuration.KeycloakDataImportConfiguration;
import org.folio.security.support.TestConfiguration;
import org.folio.test.extensions.EnableKeycloakDataImport;
import org.folio.test.extensions.EnableKeycloakTlsMode;
import org.folio.test.types.IntegrationTest;
import org.folio.tools.store.configuration.SecureStoreAutoconfiguration;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@IntegrationTest
@EnableKeycloakTlsMode
@EnableKeycloakDataImport
@SpringBootTest(classes = {
  KeycloakDataImportConfiguration.class,
  SecureStoreAutoconfiguration.class,
  TestConfiguration.class
})
class KeycloakImportServiceIT {

  private static final String CLIENT_ID = "mgr-test";
  private static final String BACKEND_ADMIN_CLIENT_ID = "folio-backend-admin-client";
  private static final String CLIENT_EXPORT = readString("json/keycloak/client.json");
  private static final String ROLES_EXPORT = readString("json/keycloak/roles.json");
  private static final String ADMIN_CLIENT_SERVICE_USER_EXPORT =
    readString("json/keycloak/backend-client-service-user-updated.json");

  @Autowired private Keycloak keycloakClient;

  @Test
  void verifyDataImportedOnAppStartup() throws JsonProcessingException, JSONException {
    var realm = keycloakClient.realm(REALM).partialExport(true, true);
    var createdRoles = getCreatedRoles(realm);
    var createdClient = getCreatedClient(realm);
    var adminClientServiceUser = getAdminClientServiceUser(realm);

    var rolesJson = OBJECT_MAPPER.writeValueAsString(createdRoles);
    var clientJson = OBJECT_MAPPER.writeValueAsString(createdClient);
    var adminServiceUserJson = OBJECT_MAPPER.writeValueAsString(adminClientServiceUser);

    JSONAssert.assertEquals(ROLES_EXPORT, rolesJson, LENIENT);
    JSONAssert.assertEquals(CLIENT_EXPORT, clientJson, LENIENT);
    // Checks that newly created roles are assigned to the service acc user of the admin client
    JSONAssert.assertEquals(ADMIN_CLIENT_SERVICE_USER_EXPORT, adminServiceUserJson, LENIENT);
  }

  private static UserRepresentation getAdminClientServiceUser(RealmRepresentation realm) {
    return realm.getUsers().stream().filter(u -> u.getServiceAccountClientId().equals(BACKEND_ADMIN_CLIENT_ID))
      .findFirst().orElseThrow();
  }

  private static ClientRepresentation getCreatedClient(RealmRepresentation realm) {
    return realm.getClients().stream().filter(c -> c.getClientId().equals(CLIENT_ID))
      .findFirst().orElseThrow();
  }

  private static List<RoleRepresentation> getCreatedRoles(RealmRepresentation realm) {
    return realm.getRoles().getRealm().stream().filter(role -> role.getAttributes().containsKey(CLIENT_ID_ATTR))
      .collect(Collectors.toList());
  }
}
