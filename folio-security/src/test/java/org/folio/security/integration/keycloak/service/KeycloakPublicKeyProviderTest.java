package org.folio.security.integration.keycloak.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakPublicKeyProviderTest {

  @InjectMocks private KeycloakPublicKeyProvider keycloakPublicKeyProvider;

  @Mock private KeycloakAuthClient keycloakAuthClient;

  @Test
  void retrievePublicKey_negative_jwkNotFound() {
    var tenant = "diku";
    var jwk = new JWK();
    jwk.setKeyId("key1");
    var jsonWebKeySet = new JSONWebKeySet();
    jsonWebKeySet.setKeys(new JWK[] {jwk});

    when(keycloakAuthClient.retrieveJwk(tenant)).thenReturn(jsonWebKeySet);

    assertThrows(IllegalArgumentException.class, () -> keycloakPublicKeyProvider.retrievePublicKey(tenant, "key2"));
  }
}