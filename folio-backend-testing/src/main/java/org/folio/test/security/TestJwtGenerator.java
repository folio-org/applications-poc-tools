package org.folio.test.security;

import static io.jsonwebtoken.JwsHeader.KEY_ID;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.folio.test.TestUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestJwtGenerator {

  private static final String TEST_KEY_PATH = "testkeys/testkey.der";
  private static final String TEST_KID = "qJr6ysS_hauNBc65Sp16ORFOqJtII3ej6uAP2-jOnuo";

  @SneakyThrows
  public static String generateExpiredJwtToken(String keycloakUrl, String tenant) {
    var expiredDate = new Date(System.currentTimeMillis() - DateUtils.MILLIS_PER_HOUR);
    return generateJwtToken(keycloakUrl, tenant, expiredDate, expiredDate);
  }

  @SneakyThrows
  public static String generateJwtToken(String keycloakUrl, String tenant) {
    var now = new Date();
    return generateJwtToken(keycloakUrl, tenant, now, DateUtils.addDays(now, 1));
  }

  private static String generateJwtToken(String keycloakUrl, String tenant, Date issuedAt, Date expiration)
    throws Exception {
    var key = readPrivateKey(TestUtils.readStream(TEST_KEY_PATH));

    return Jwts.builder()
      .setHeaderParam(KEY_ID, TEST_KID)
      .setSubject(UUID.randomUUID().toString())
      .setIssuer(keycloakUrl + "/realms/" + tenant)
      .setIssuedAt(issuedAt)
      .setExpiration(expiration)
      .signWith(SignatureAlgorithm.RS256, key)
      .compact();
  }

  private static RSAPrivateKey readPrivateKey(InputStream keyStream) throws Exception {
    byte[] keyBytes = IOUtils.toByteArray(keyStream);

    var spec = new PKCS8EncodedKeySpec(keyBytes);
    var keyFactory = KeyFactory.getInstance("RSA");

    return (RSAPrivateKey) keyFactory.generatePrivate(spec);
  }
}
