package org.folio.security.integration.keycloak.client;

import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import org.folio.security.integration.keycloak.model.TokenResponse;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange
public interface KeycloakAuthClient {

  @PostExchange(value = "/realms/master/protocol/openid-connect/token", contentType = APPLICATION_FORM_URLENCODED_VALUE)
  TokenResponse evaluatePermissions(@RequestBody MultiValueMap<String, String> formData,
    @RequestHeader("Authorization") String authToken);

  @GetExchange(value = "/realms/{realm}/protocol/openid-connect/certs")
  JSONWebKeySet retrieveJwk(@PathVariable String realm);
}
