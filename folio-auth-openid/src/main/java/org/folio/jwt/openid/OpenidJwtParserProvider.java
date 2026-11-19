package org.folio.jwt.openid;

import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLContext;
import lombok.extern.log4j.Log4j2;
import org.folio.jwt.openid.tls.JwksTlsAuthContextInfo;
import org.folio.jwt.openid.tls.JwksTlsCallerPrincipalFactory;

@Log4j2
public class OpenidJwtParserProvider {

  private final Map<String, JWTParser> tokenParsers = new ConcurrentHashMap<>();
  private final int jwksRefreshInterval;
  private final int forcedJwksRefreshInterval;
  private final String jwksKeycloakBaseUrl;
  private final SSLContext sslContext;

  /**
   * Constructor with all parameters.
   *
   * @param jwksRefreshInterval - JWKS refresh interval
   * @param forcedJwksRefreshInterval - forced JWKS refresh interval
   * @param jwksKeycloakBaseUrl - custom Keycloak base URL for JWKS endpoint
   * @param sslContext - SSL context used by the JWKS HTTP client, or {@code null} to use the JVM default trust
   */
  public OpenidJwtParserProvider(int jwksRefreshInterval, int forcedJwksRefreshInterval,
    String jwksKeycloakBaseUrl, SSLContext sslContext) {
    this.jwksRefreshInterval = jwksRefreshInterval;
    this.forcedJwksRefreshInterval = forcedJwksRefreshInterval;
    this.jwksKeycloakBaseUrl = jwksKeycloakBaseUrl;
    this.sslContext = sslContext;
  }

  /**
   * Constructor without custom SSL context, the JVM default trust is used by the JWKS HTTP client.
   *
   * @param jwksRefreshInterval - JWKS refresh interval
   * @param forcedJwksRefreshInterval - forced JWKS refresh interval
   * @param jwksKeycloakBaseUrl - custom Keycloak base URL for JWKS endpoint
   */
  public OpenidJwtParserProvider(int jwksRefreshInterval, int forcedJwksRefreshInterval,
    String jwksKeycloakBaseUrl) {
    this(jwksRefreshInterval, forcedJwksRefreshInterval, jwksKeycloakBaseUrl, null);
  }

  /**
   * Constructor for backward compatibility.
   *
   * @param jwksRefreshInterval - JWKS refresh interval
   * @param forcedJwksRefreshInterval - forced JWKS refresh interval
   */
  public OpenidJwtParserProvider(int jwksRefreshInterval, int forcedJwksRefreshInterval) {
    this(jwksRefreshInterval, forcedJwksRefreshInterval, null);
  }

  /**
   * Provides JWT parser for given issuer URI.
   *
   * @param issuerUri - JWT token issuer
   * @return corresponding JWT Parser for the given issuer URI.
   */
  public JWTParser getParser(String issuerUri) {
    var jwtTokenParserProvider = tokenParsers.get(issuerUri);
    if (jwtTokenParserProvider != null) {
      return jwtTokenParserProvider;
    }

    var jwksUrl = buildJwksUrl(issuerUri);
    log.debug("Creating JWT parser for issuer: {}, JWKS URL: {}", issuerUri, jwksUrl);

    var jwtAuthContextInfo = createAuthContextInfo(jwksUrl, issuerUri);
    jwtAuthContextInfo.setJwksRefreshInterval(jwksRefreshInterval);
    jwtAuthContextInfo.setForcedJwksRefreshInterval(forcedJwksRefreshInterval);
    var jwtParser = createParser(jwtAuthContextInfo);
    tokenParsers.put(issuerUri, jwtParser);
    return jwtParser;
  }

  private JWTAuthContextInfo createAuthContextInfo(String jwksUrl, String issuerUri) {
    return sslContext == null
      ? new JWTAuthContextInfo(jwksUrl, issuerUri)
      : new JwksTlsAuthContextInfo(jwksUrl, issuerUri, sslContext);
  }

  private JWTParser createParser(JWTAuthContextInfo jwtAuthContextInfo) {
    // a parser has its own key resolver, therefore the factory must not be shared between parsers
    return sslContext == null
      ? new DefaultJWTParser(jwtAuthContextInfo)
      : new DefaultJWTParser(jwtAuthContextInfo, new JwksTlsCallerPrincipalFactory());
  }

  /**
   * Builds JWKS URL based on issuer URI and custom base URL if specified.
   *
   * @param issuerUri - JWT token issuer URI
   * @return JWKS endpoint URL
   */
  private String buildJwksUrl(String issuerUri) {
    if (jwksKeycloakBaseUrl != null && !jwksKeycloakBaseUrl.isBlank()) {
      var realm = resolveTenant(issuerUri);
      var baseUrl = jwksKeycloakBaseUrl.endsWith("/")
        ? jwksKeycloakBaseUrl.substring(0, jwksKeycloakBaseUrl.length() - 1)
        : jwksKeycloakBaseUrl;

      var customUrl = baseUrl + "/realms/" + realm + "/protocol/openid-connect/certs";
      log.debug("Using custom Keycloak base URL for JWKS: {} (original issuer: {})", customUrl, issuerUri);
      return customUrl;
    }

    return issuerUri + "/protocol/openid-connect/certs";
  }

  /**
   * Invalidates all cache entries.
   */
  public void invalidateCache() {
    tokenParsers.clear();
  }

  /**
   * Invalidates cache entry by key.
   */
  public void invalidateCache(String issuerUri) {
    tokenParsers.remove(issuerUri);
  }

  /**
   * Invalidates caches entries for the given collection of tenants.
   *
   * @param tenants - collection of tenants to be kept in cache
   */
  public void invalidateCache(Collection<String> tenants) {
    log.info("Invalidating outdated token parsers");
    if (tenants == null || tenants.isEmpty()) {
      tokenParsers.clear();
      return;
    }

    tokenParsers.keySet().stream()
      .filter(issuer -> !tenants.contains(resolveTenant(issuer)))
      .forEach(tokenParsers::remove);
  }

  private static String resolveTenant(String issuer) {
    return issuer.substring(issuer.lastIndexOf('/') + 1);
  }
}
