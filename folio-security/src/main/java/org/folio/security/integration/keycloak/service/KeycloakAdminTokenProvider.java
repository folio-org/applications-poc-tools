package org.folio.security.integration.keycloak.service;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.Instant;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Acquires and caches the Keycloak admin access token used to authenticate {@code KeycloakAdminClient}
 * calls. Replaces the token lifecycle that {@code keycloak-admin-client} handled internally: it requests
 * a token from the {@code master} realm token endpoint with the configured admin grant and refreshes it
 * shortly before expiry.
 */
@Log4j2
public class KeycloakAdminTokenProvider {

  private static final long EXPIRY_SKEW_SECONDS = 30L;

  private final KeycloakAuthClient authClient;
  private final KeycloakProperties properties;
  private final Supplier<String> clientSecretSupplier;

  private String accessToken;
  private Instant expiresAt = Instant.MIN;

  public KeycloakAdminTokenProvider(KeycloakAuthClient authClient, KeycloakProperties properties,
    Supplier<String> clientSecretSupplier) {
    this.authClient = authClient;
    this.properties = properties;
    this.clientSecretSupplier = clientSecretSupplier;
  }

  /**
   * Returns a valid admin access token, fetching a new one if none is cached or the cached one is expired.
   */
  public synchronized String getAccessToken() {
    if (accessToken == null || Instant.now().isAfter(expiresAt)) {
      refresh();
    }
    return accessToken;
  }

  private void refresh() {
    var admin = properties.getAdmin();
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", admin.getGrantType());
    form.add("client_id", admin.getClientId());
    var secret = clientSecretSupplier.get();
    if (isNotBlank(secret)) {
      form.add("client_secret", secret);
    }
    if (isNotBlank(admin.getUsername())) {
      form.add("username", admin.getUsername());
    }
    if (isNotBlank(admin.getPassword())) {
      form.add("password", admin.getPassword());
    }

    log.debug("Obtaining Keycloak admin token: clientId={}, grantType={}", admin.getClientId(), admin.getGrantType());
    var response = authClient.obtainToken(form);
    this.accessToken = response.getAccessToken();
    var ttl = response.getExpiresIn() != null ? response.getExpiresIn() : 0L;
    this.expiresAt = Instant.now().plusSeconds(Math.max(0L, ttl - EXPIRY_SKEW_SECONDS));
  }
}
