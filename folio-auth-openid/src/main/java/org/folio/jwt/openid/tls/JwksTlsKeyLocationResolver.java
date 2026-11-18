package org.folio.jwt.openid.tls;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.KeyLocationResolver;
import org.jose4j.http.Get;
import org.jose4j.lang.UnresolvableKeyException;

/**
 * {@link KeyLocationResolver} that retrieves JWKS using the SSL socket factory of {@link JwksTlsAuthContextInfo}.
 *
 * <p>The HTTP client is requested by the super constructor, so the SSL configuration cannot be a field of this class
 * and is read from the auth context info instead.</p>
 */
public class JwksTlsKeyLocationResolver extends KeyLocationResolver {

  public JwksTlsKeyLocationResolver(JWTAuthContextInfo authContextInfo) throws UnresolvableKeyException {
    super(authContextInfo);
  }

  @Override
  protected Get getHttpGet() {
    var httpGet = super.getHttpGet();
    if (authContextInfo instanceof JwksTlsAuthContextInfo tlsAuthContextInfo) {
      httpGet.setSslSocketFactory(tlsAuthContextInfo.getSslSocketFactory());
    }
    return httpGet;
  }
}
