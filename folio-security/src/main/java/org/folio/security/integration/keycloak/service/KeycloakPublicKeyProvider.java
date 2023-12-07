package org.folio.security.integration.keycloak.service;

import com.auth0.jwk.UrlJwkProvider;
import java.net.URL;
import java.security.PublicKey;
import lombok.RequiredArgsConstructor;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.springframework.cache.annotation.Cacheable;

@RequiredArgsConstructor
public class KeycloakPublicKeyProvider {

  private final KeycloakProperties properties;

  @Cacheable(cacheNames = "keycloak-jwk", key = "#realm-#keyId")
  public PublicKey retrievePublicKey(String realm, String keyId) throws Exception {
    var url = new URL(properties.getUrl() + "/realms/" + realm + "/protocol/openid-connect/certs");
    var provider = new UrlJwkProvider(url);
    var jwk = provider.get(keyId);
    return jwk.getPublicKey();
  }
}
