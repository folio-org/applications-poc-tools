package org.folio.security.integration.keycloak.service;

import lombok.RequiredArgsConstructor;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.keycloak.TokenVerifier;
import org.keycloak.exceptions.TokenNotActiveException;
import org.keycloak.representations.AccessToken;

@RequiredArgsConstructor
public class KeycloakTokenValidator {

  private final KeycloakPublicKeyProvider publicKeyProvider;
  private final KeycloakProperties properties;

  public AccessToken validateAndDecodeToken(String token) {
    try {
      token = trimTokenBearer(token);
      var tokenVerifier = TokenVerifier.create(token, AccessToken.class);
      var accessToken = tokenVerifier.getToken();
      var keyId = tokenVerifier.getHeader().getKeyId();
      var realmName = resolveTenant(accessToken.getIssuer());
      var publicKey = publicKeyProvider.retrievePublicKey(realmName, keyId);
      var realmUrl = buildRealmUrl(realmName);

      tokenVerifier.publicKey(publicKey).realmUrl(realmUrl).checkRealmUrl(true).checkActive(true).verify();

      return accessToken;
    } catch (TokenNotActiveException e) {
      throw new NotAuthorizedException("JWT token expired");
    } catch (Exception e) {
      throw new NotAuthorizedException("Failed to validate a token", e);
    }
  }

  public static String resolveTenant(String tokenIssuer) {
    return tokenIssuer.substring(tokenIssuer.lastIndexOf('/') + 1);
  }

  private static String trimTokenBearer(String token) {
    return token == null || !token.startsWith("Bearer ") ? token : token.substring(7).trim();
  }

  private String buildRealmUrl(String realmName) {
    var urlBuilder = new StringBuilder(properties.getUrl());
    var baseUrl = properties.getUrl();
    if (!baseUrl.endsWith("/")) {
      urlBuilder.append("/");
    }
    return urlBuilder.append("realms/").append(realmName).toString();
  }
}
