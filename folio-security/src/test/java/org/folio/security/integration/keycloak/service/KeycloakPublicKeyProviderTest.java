package org.folio.security.integration.keycloak.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;
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

  @Test
  void retrievePublicKey_positive() {
    var keyId = "nBkWxZA0zEBvJGB5Kc8Vk4EDVF1EZNOC";
    var jwk = new JWK();
    jwk.setKeyId(keyId);
    jwk.setKeyType("RSA");
    jwk.setAlgorithm("RS256");
    jwk.setPublicKeyUse("sig");
    jwk.setOtherClaims("n", "dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGVzdHRlc3R0ZXN0dGV0ZXN0dHNldHRlc3R0ZXN0");
    jwk.setOtherClaims("e", "AQAB");
    jwk.setOtherClaims("x5c", "I5MTI1MDA2WhcNMzMwODI5MTI1CSqGSIb3DQEBAQUAA4IBDwAwggEKAo");
    jwk.setOtherClaims("x5t", "QCq5XoogegV9LO46WkS0OUYDLScLM+eJrvtIvXOxwLDiiYQ9GZUJZjq156+SkJJjcW/A1ks");
    jwk.setOtherClaims("x5t", "QCq5XoogegV9LO46WkS0OUYDLScLM+eJrvtIvXOxwLDiiYQ9GZUJZjq156+SkJJjcW/A1ks");
    jwk.setOtherClaims("x5t#S256", "ltBacS-gNy7R");
    var jsonWebKeySet = new JSONWebKeySet();
    jsonWebKeySet.setKeys(new JWK[] {jwk});
    var tenant = "diku";

    when(keycloakAuthClient.retrieveJwk(tenant)).thenReturn(jsonWebKeySet);

    var expectedPublicKey = JWKParser.create(jwk).toPublicKey();
    var publicKey = keycloakPublicKeyProvider.retrievePublicKey(tenant, keyId);
    Assertions.assertThat(publicKey).isEqualTo(expectedPublicKey);
  }
}
