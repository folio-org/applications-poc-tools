package org.folio.security.integration.keycloak.service;

import static java.util.Arrays.stream;

import java.security.PublicKey;
import lombok.RequiredArgsConstructor;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.keycloak.jose.jwk.JWKParser;
import org.springframework.cache.annotation.Cacheable;

@RequiredArgsConstructor
public class KeycloakPublicKeyProvider {

  private final KeycloakAuthClient keycloakClient;

  @Cacheable(cacheNames = "keycloak-jwk", key = "#realm-#keyId")
  public PublicKey retrievePublicKey(String realm, String keyId) throws Exception {
    var jsonWebKeySet = keycloakClient.retrieveJwk(realm);
    var jwk = stream(jsonWebKeySet.getKeys())
      .filter(j -> j.getKeyId().equals(keyId))
      .findFirst()
      .orElseThrow(() -> new IllegalArgumentException("Key not found"));
    return JWKParser.create(jwk).toPublicKey();
  }
}
