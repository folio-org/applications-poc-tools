package org.folio.jwt.openid.tls;

import io.smallrye.jwt.KeyProvider;
import io.smallrye.jwt.auth.principal.DefaultJWTTokenParser;
import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.UnresolvableKeyException;

/**
 * {@link DefaultJWTTokenParser} that resolves verification keys with {@link JwksTlsKeyLocationResolver}, so JWKS is
 * retrieved using the custom trust configuration.
 */
public class JwksTlsTokenParser extends DefaultJWTTokenParser {

  /**
   * The reference is published once under a lock, and the resolver is thread-safe.
   * This keeps the lock-free fast path on every token parse.
   */
  @SuppressWarnings("java:S3077")
  private volatile VerificationKeyResolver keyResolver;

  @Override
  protected VerificationKeyResolver getVerificationKeyResolver(JWTAuthContextInfo authContextInfo)
    throws UnresolvableKeyException {
    if (!isSupported(authContextInfo)) {
      return super.getVerificationKeyResolver(authContextInfo);
    }

    if (keyResolver == null) {
      synchronized (this) {
        if (keyResolver == null) {
          keyResolver = new JwksTlsKeyLocationResolver(authContextInfo);
        }
      }
    }
    return keyResolver;
  }

  private static boolean isSupported(JWTAuthContextInfo authContextInfo) {
    return authContextInfo instanceof JwksTlsAuthContextInfo
      && authContextInfo.getKeyProvider() != KeyProvider.AWS_ALB
      && !authContextInfo.isVerifyCertificateThumbprint();
  }
}
