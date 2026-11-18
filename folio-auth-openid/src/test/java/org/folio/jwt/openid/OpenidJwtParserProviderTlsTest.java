package org.folio.jwt.openid;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.smallrye.jwt.auth.principal.ParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.KeyStore;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;
import lombok.SneakyThrows;
import org.folio.test.types.UnitTest;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.jwk.RsaJwkGenerator;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.NumericDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that JWKS is retrieved over HTTPS using the provided {@link SSLContext} only.
 */
@UnitTest
class OpenidJwtParserProviderTlsTest {

  private static final String KEYSTORE_PATH = "tls/jwks-server.p12";
  private static final String KEYSTORE_PASSWORD = "changeit";
  private static final String TENANT = "test";
  private static final String ISSUER = "https://keycloak.example.org/realms/" + TENANT;
  private static final String KEY_ID = "test-key-id";

  private final AtomicInteger jwksRequests = new AtomicInteger();

  private SSLServerSocket jwksServerSocket;
  private RsaJsonWebKey signingKey;
  private String jwksBaseUrl;

  @BeforeEach
  void setUp() throws Exception {
    signingKey = RsaJwkGenerator.generateJwk(2048);
    signingKey.setKeyId(KEY_ID);
    startJwksServer();
  }

  @AfterEach
  void tearDown() throws IOException {
    jwksServerSocket.close();
  }

  @Test
  void getParser_positive_jwksCertificateTrustedByCustomSslContext() throws Exception {
    var defaultSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
    var defaultSslContext = SSLContext.getDefault();
    var provider = new OpenidJwtParserProvider(60, 60, jwksBaseUrl, trustingSslContext());

    var token = signedToken(ISSUER);
    var jwt = provider.getParser(ISSUER).parse(token);

    assertThat(jwt.getSubject()).isEqualTo("test-subject");
    assertThat(jwt.getIssuer()).isEqualTo(ISSUER);
    assertThat(jwksRequests).hasValue(1);
    assertThat(HttpsURLConnection.getDefaultSSLSocketFactory()).isSameAs(defaultSocketFactory);
    assertThat(SSLContext.getDefault()).isSameAs(defaultSslContext);
  }

  @Test
  void getParser_positive_jwksIsCachedBetweenParseCalls() throws Exception {
    var provider = new OpenidJwtParserProvider(60, 60, jwksBaseUrl, trustingSslContext());
    var parser = provider.getParser(ISSUER);

    parser.parse(signedToken(ISSUER));
    parser.parse(signedToken(ISSUER));

    assertThat(provider.getParser(ISSUER)).isSameAs(parser);
    assertThat(jwksRequests).hasValue(1);
  }

  @Test
  void getParser_negative_jwksCertificateNotTrustedWithoutSslContext() {
    var provider = new OpenidJwtParserProvider(60, 60, jwksBaseUrl);

    var token = signedToken(ISSUER);
    var parser = provider.getParser(ISSUER);

    assertThatThrownBy(() -> parser.parse(token)).isInstanceOf(ParseException.class);
    assertThat(jwksRequests).hasValue(0);
  }

  @Test
  void getParser_negative_jwksCertificateNotTrustedWithNullSslContext() {
    var provider = new OpenidJwtParserProvider(60, 60, jwksBaseUrl, null);

    var token = signedToken(ISSUER);
    var parser = provider.getParser(ISSUER);

    assertThatThrownBy(() -> parser.parse(token)).isInstanceOf(ParseException.class);
    assertThat(jwksRequests).hasValue(0);
  }

  @Test
  void getParser_negative_jwksCertificateNotTrustedByUnrelatedTruststore() throws Exception {
    var emptyTrustStore = KeyStore.getInstance("PKCS12");
    emptyTrustStore.load(null, null);
    var provider = new OpenidJwtParserProvider(60, 60, jwksBaseUrl, sslContext(emptyTrustStore));

    var token = signedToken(ISSUER);
    var parser = provider.getParser(ISSUER);

    assertThatThrownBy(() -> parser.parse(token)).isInstanceOf(ParseException.class);
    assertThat(jwksRequests).hasValue(0);
  }

  private void startJwksServer() throws Exception {
    var keyStore = loadServerKeyStore();
    var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    keyManagerFactory.init(keyStore, KEYSTORE_PASSWORD.toCharArray());
    var serverSslContext = SSLContext.getInstance("TLS");
    serverSslContext.init(keyManagerFactory.getKeyManagers(), null, null);

    var jwks = new JsonWebKeySet(signingKey).toJson(JsonWebKey.OutputControlLevel.PUBLIC_ONLY).getBytes(UTF_8);

    jwksServerSocket = (SSLServerSocket) serverSslContext.getServerSocketFactory()
      .createServerSocket(0, 10, InetAddress.getLoopbackAddress());
    var serverThread = new Thread(() -> serveJwks(jwks), "jwks-test-server");
    serverThread.setDaemon(true);
    serverThread.start();

    jwksBaseUrl = "https://localhost:" + jwksServerSocket.getLocalPort();
  }

  /**
   * Minimal HTTPS server that answers every request with the JWKS. A plain socket is used, because the JDK
   * HttpServer requires NIO loopback pipes that are not available in every build environment.
   */
  private void serveJwks(byte[] jwks) {
    while (!jwksServerSocket.isClosed()) {
      try (var socket = jwksServerSocket.accept()) {
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
          // skip request line and headers
        }
        jwksRequests.incrementAndGet();
        var headers = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: " + jwks.length
          + "\r\nConnection: close\r\n\r\n";
        var out = socket.getOutputStream();
        out.write(headers.getBytes(UTF_8));
        out.write(jwks);
        out.flush();
      } catch (SocketException e) {
        // server socket is closed
      } catch (IOException e) {
        // TLS handshake rejected by the client, no request is served
      }
    }
  }

  private static SSLContext trustingSslContext() throws Exception {
    var trustStore = KeyStore.getInstance("PKCS12");
    trustStore.load(null, null);
    var serverKeyStore = loadServerKeyStore();
    var alias = serverKeyStore.aliases().nextElement();
    trustStore.setCertificateEntry(alias, serverKeyStore.getCertificate(alias));
    return sslContext(trustStore);
  }

  private static SSLContext sslContext(KeyStore trustStore) throws Exception {
    var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    trustManagerFactory.init(trustStore);
    var sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
    return sslContext;
  }

  private static KeyStore loadServerKeyStore() throws Exception {
    var keyStore = KeyStore.getInstance("PKCS12");
    try (InputStream is = OpenidJwtParserProviderTlsTest.class.getClassLoader().getResourceAsStream(KEYSTORE_PATH)) {
      keyStore.load(is, KEYSTORE_PASSWORD.toCharArray());
    }
    return keyStore;
  }

  @SneakyThrows
  private String signedToken(String issuer) {
    var claims = new JwtClaims();
    claims.setIssuer(issuer);
    claims.setSubject("test-subject");
    claims.setIssuedAtToNow();
    claims.setExpirationTime(NumericDate.fromSeconds(NumericDate.now().getValue() + 600));

    var jws = new JsonWebSignature();
    jws.setPayload(claims.toJson());
    jws.setKey(signingKey.getPrivateKey());
    jws.setKeyIdHeaderValue(KEY_ID);
    jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
    return jws.getCompactSerialization();
  }
}
