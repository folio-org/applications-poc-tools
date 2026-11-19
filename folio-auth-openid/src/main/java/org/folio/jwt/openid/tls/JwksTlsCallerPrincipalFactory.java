package org.folio.jwt.openid.tls;

import io.smallrye.jwt.auth.principal.DefaultJWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.jwt.auth.principal.JWTCallerPrincipalFactory;
import io.smallrye.jwt.auth.principal.ParseException;

/**
 * {@link JWTCallerPrincipalFactory} that parses tokens with {@link JwksTlsTokenParser}.
 */
public class JwksTlsCallerPrincipalFactory extends JWTCallerPrincipalFactory {

  private final JwksTlsTokenParser parser = new JwksTlsTokenParser();

  @Override
  public JWTCallerPrincipal parse(String token, JWTAuthContextInfo authContextInfo) throws ParseException {
    var jwtContext = parser.parse(token, authContextInfo);
    String type = jwtContext.getJoseObjects().get(0).getHeader("typ");
    return new DefaultJWTCallerPrincipal(type, jwtContext.getJwtClaims());
  }
}
