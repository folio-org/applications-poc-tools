package org.folio.security.integration.keycloak.service;

import static org.folio.security.integration.keycloak.service.KeycloakImportService.DESCRIPTOR_HASH_ATTR;
import static org.folio.security.integration.keycloak.service.KeycloakImportService.REALM;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakAdminProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakClientProperties;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith({MockitoExtension.class})
class KeycloakImportServiceTest {

  @InjectMocks private KeycloakImportService keycloakImportService;
  @Mock private Keycloak keycloakClient;
  @Mock private KeycloakProperties props;
  @Mock private InternalModuleDescriptorProvider descriptorProvider;
  @Spy private KeycloakModuleDescriptorMapper mapper;

  @Test
  void importData_positive_existingClientOutdated() {
    var keycloakClientProperties = new KeycloakClientProperties();
    keycloakClientProperties.setClientId("clientId");
    var keycloakAdminProperties = new KeycloakAdminProperties();
    keycloakAdminProperties.setClientId("adminClientId");
    var clientRepresentation = new ClientRepresentation();
    clientRepresentation.setClientId(keycloakClientProperties.getClientId());
    clientRepresentation.setId("clientRepresentationId");
    var clientOutdatedHash = "0";
    var clientAttributes = Map.of(DESCRIPTOR_HASH_ATTR, clientOutdatedHash);
    clientRepresentation.setAttributes(clientAttributes);
    var adminClientRepresentation = new ClientRepresentation();
    adminClientRepresentation.setClientId(keycloakAdminProperties.getClientId());
    adminClientRepresentation.setId("adminClientRepresentationId");
    var moduleDescriptor = new ModuleDescriptor();
    var realmResource = Mockito.mock(RealmResource.class);
    var clientsResource = Mockito.mock(ClientsResource.class);
    var clientResource = Mockito.mock(ClientResource.class);
    var rolesResource = Mockito.mock(RolesResource.class);
    var response = Mockito.mock(Response.class);

    when(descriptorProvider.getModuleDescriptor()).thenReturn(moduleDescriptor);
    when(props.getClient()).thenReturn(keycloakClientProperties);
    when(keycloakClient.realm(REALM)).thenReturn(realmResource);
    when(realmResource.clients()).thenReturn(clientsResource);
    when(clientsResource.findByClientId("clientId")).thenReturn(List.of(clientRepresentation));
    when(clientsResource.get("clientRepresentationId")).thenReturn(clientResource);
    when(realmResource.roles()).thenReturn(rolesResource);
    when(rolesResource.list(false)).thenReturn(List.of());
    when(clientsResource.create(isA(ClientRepresentation.class))).thenReturn(response);
    when(props.getAdmin()).thenReturn(keycloakAdminProperties);
    when(clientsResource.findByClientId("adminClientId")).thenReturn(List.of(adminClientRepresentation));

    keycloakImportService.importData();

    verify(clientResource).remove();
    verify(keycloakClient).proxy(eq(UserResource.class), isA(URI.class));
  }
}
