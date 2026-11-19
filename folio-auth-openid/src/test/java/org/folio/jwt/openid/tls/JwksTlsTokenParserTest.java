package org.folio.jwt.openid.tls;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.KeyLocationResolver;
import java.security.KeyPairGenerator;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import lombok.SneakyThrows;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class JwksTlsTokenParserTest {

  private static final String ISSUER = "https://keycloak.example.org/realms/test";
  private static final String JWKS_URL = "https://keycloak.example.org/realms/test/protocol/openid-connect/certs";

  private final JwksTlsTokenParser parser = new JwksTlsTokenParser();

  @Test
  @SneakyThrows
  void getVerificationKeyResolver_positive_plainAuthContextInfoFallsBackToDefaultResolver() {
    var authContextInfo = new JWTAuthContextInfo();
    authContextInfo.setIssuedBy(ISSUER);
    authContextInfo.setPublicKeyContent(publicKeyPem());

    var resolver = parser.getVerificationKeyResolver(authContextInfo);

    assertThat(resolver).isExactlyInstanceOf(KeyLocationResolver.class);
  }

  @Test
  @SneakyThrows
  void authContextInfo_positive_exposesSocketFactoryOfSslContext() {
    var authContextInfo = new JwksTlsAuthContextInfo(JWKS_URL, ISSUER, SSLContext.getDefault());

    assertThat(authContextInfo.getSslSocketFactory()).isNotNull();
    assertThat(authContextInfo.getPublicKeyLocation()).isEqualTo(JWKS_URL);
    assertThat(authContextInfo.getIssuedBy()).isEqualTo(ISSUER);
  }

  @SneakyThrows
  private static String publicKeyPem() {
    var publicKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPublic();
    var encoded = Base64.getMimeEncoder(64, "\n".getBytes(UTF_8)).encodeToString(publicKey.getEncoded());
    return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----";
  }
}
