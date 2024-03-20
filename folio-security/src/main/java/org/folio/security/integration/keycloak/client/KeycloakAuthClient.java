package org.folio.security.integration.keycloak.client;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;

import java.util.Map;
import org.folio.security.integration.keycloak.model.TokenResponse;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface KeycloakAuthClient {

  @PostMapping(value = "/realms/master/protocol/openid-connect/token", consumes = APPLICATION_FORM_URLENCODED_VALUE)
  TokenResponse evaluatePermissions(@RequestBody Map<String, ?> formData,
    @RequestHeader("Authorization") String authToken);

  @GetMapping(value = "/realms/{realm}/protocol/openid-connect/certs")
  JSONWebKeySet retrieveJwk(@PathVariable String realm);
}
