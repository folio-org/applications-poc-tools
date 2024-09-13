package org.folio.jwt.openid;

import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RequiredArgsConstructor
public class OpenidJwtParserProvider {

  private final Map<String, JWTParser> tokenParsers = new ConcurrentHashMap<>();
  private final int jwksRefreshInterval;
  private final int forcedJwksRefreshInterval;

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

    var jwtAuthContextInfo = new JWTAuthContextInfo(issuerUri + "/protocol/openid-connect/certs", issuerUri);
    jwtAuthContextInfo.setJwksRefreshInterval(jwksRefreshInterval);
    jwtAuthContextInfo.setForcedJwksRefreshInterval(forcedJwksRefreshInterval);
    var jwtParser = new DefaultJWTParser(jwtAuthContextInfo);
    tokenParsers.put(issuerUri, jwtParser);
    return jwtParser;
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
   * Invalidates caches entries for the given list of tenants.
   *
   * @param tenants - list of tenants to invalidate cache entries
   */
  public void invalidateCache(List<String> tenants) {
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
