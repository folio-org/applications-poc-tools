package org.folio.jwt.openid.tls;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * {@link JWTAuthContextInfo} that additionally carries the {@link SSLContext} to be used by the JWKS HTTP client.
 *
 * <p>SmallRye JWT can only trust a single PEM certificate for the JWKS endpoint, so the custom trust configuration
 * is passed to {@link JwksTlsKeyLocationResolver} through this class instead.</p>
 */
public class JwksTlsAuthContextInfo extends JWTAuthContextInfo {

  private final SSLSocketFactory sslSocketFactory;

  /**
   * Creates context info for the JWKS location.
   *
   * @param jwksUrl - JWKS endpoint URL
   * @param issuer - expected token issuer
   * @param sslContext - SSL context to be used when JWKS is retrieved via HTTPS
   */
  public JwksTlsAuthContextInfo(String jwksUrl, String issuer, SSLContext sslContext) {
    super(jwksUrl, issuer);
    this.sslSocketFactory = sslContext.getSocketFactory();
  }

  public SSLSocketFactory getSslSocketFactory() {
    return sslSocketFactory;
  }
}
