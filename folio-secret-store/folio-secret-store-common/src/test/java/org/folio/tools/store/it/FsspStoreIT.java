package org.folio.tools.store.it;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.apache.commons.lang3.SystemProperties.JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.test.TestUtils.parse;
import static org.folio.test.TestUtils.readString;
import static org.folio.tools.store.support.SecretStoreTestValues.FSSP_CLIENT_KEYSTORE_PATH;
import static org.folio.tools.store.support.SecretStoreTestValues.FSSP_CLIENT_TRUSTSTORE_PATH;
import static org.folio.tools.store.support.SecretStoreTestValues.FSSP_SERVER_KEYSTORE_PATH;
import static org.folio.tools.store.support.SecretStoreTestValues.FSSP_SERVER_TRUSTSTORE_PATH;
import static org.folio.tools.store.support.SecretStoreTestValues.KS_FILE_TYPE_PKCS12;
import static org.folio.tools.store.support.SecretStoreTestValues.KS_PASSWORD;

import com.github.tomakehurst.wiremock.common.Slf4jNotifier;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.io.FileNotFoundException;
import org.folio.test.extensions.LogTestMethod;
import org.folio.test.types.IntegrationTest;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.folio.tools.store.exception.SecureStoreServiceException;
import org.folio.tools.store.impl.FsspStore;
import org.folio.tools.store.properties.FsspConfigProperties;
import org.folio.tools.store.utils.ResourceUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@IntegrationTest
@LogTestMethod
class FsspStoreIT {

  private static final String KEY1 = "key1";
  private static final String VALUE1 = "value1";

  private static final String SERVER_KEYSTORE_PATH;
  private static final String SERVER_TRUSTSTORE_PATH;

  static {
    try {
      var ksFile = ResourceUtils.getFile(FSSP_SERVER_KEYSTORE_PATH);
      SERVER_KEYSTORE_PATH = ksFile.getAbsolutePath();

      var tsFile = ResourceUtils.getFile(FSSP_SERVER_TRUSTSTORE_PATH);
      SERVER_TRUSTSTORE_PATH = tsFile.getAbsolutePath();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  @RegisterExtension
  static WireMockExtension wm = WireMockExtension.newInstance()
    .options(wireMockConfig()
      .dynamicHttpsPort()
      .notifier(new Slf4jNotifier(true))
      // Disable HTTP, because FSSP server supports only HTTPS
      // and we want to make sure that client works only via HTTPS
      .httpDisabled(true)
      // Configure WireMock to use TLS with mutual authentication
      // using test keystore and truststore
      // - configure WireMock server to use keystore with private key
      .keystorePath(SERVER_KEYSTORE_PATH)
      .keystorePassword(KS_PASSWORD)
      .keyManagerPassword(KS_PASSWORD)
      .keystoreType(KS_FILE_TYPE_PKCS12)
      // - configure WireMock server to use truststore with trusted certs
      .trustStorePath(SERVER_TRUSTSTORE_PATH)
      .trustStorePassword(KS_PASSWORD)
      .trustStoreType(KS_FILE_TYPE_PKCS12)
      // - require client authentication
      .needClientAuth(true)
    )
    .failOnUnmatchedRequests(true)
    .resetOnEachTest(true)
    .build();

  private FsspStore fsspStore;

  @BeforeAll
  static void beforeAll() {
    System.setProperty(JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION, "true");
  }

  @AfterAll
  static void afterAll() {
    System.clearProperty(JDK_INTERNAL_HTTP_CLIENT_DISABLE_HOST_NAME_VERIFICATION);
  }

  @BeforeEach
  void setUp() {
    var properties = FsspConfigProperties.builder()
      .address(wm.baseUrl())
      .secretPath("secure-store/entries")
      .keyStorePath(FSSP_CLIENT_KEYSTORE_PATH)
      .keyStorePassword(KS_PASSWORD)
      .keyStoreFileType(KS_FILE_TYPE_PKCS12)
      .trustStorePath(FSSP_CLIENT_TRUSTSTORE_PATH)
      .trustStorePassword(KS_PASSWORD)
      .trustStoreFileType(KS_FILE_TYPE_PKCS12)
      .build();

    fsspStore = new FsspStore(properties);
  }

  @Test
  void get_positive() {
    addStubMapping("wiremock/stubs/get-key1-secret.json");

    var actual = fsspStore.get(KEY1);
    assertThat(actual).isEqualTo(VALUE1);
  }

  @Test
  void get_negative_notFound() {
    addStubMapping("wiremock/stubs/get-key1-secret-not-found.json");

    assertThatThrownBy(() -> fsspStore.get(KEY1))
      .isInstanceOf(SecretNotFoundException.class)
      .hasMessageContaining("Secret not found: key = " + KEY1);
  }

  @Test
  void get_negative_internalServerError() {
    addStubMapping("wiremock/stubs/secret-server-issue.json");

    assertThatThrownBy(() -> fsspStore.get(KEY1))
      .isInstanceOf(SecureStoreServiceException.class)
      .hasMessageContaining("Failed to get secret: key = " + KEY1);
  }

  @Test
  void set_positive() {
    addStubMapping("wiremock/stubs/put-key1-secret.json");

    fsspStore.set(KEY1, VALUE1);
    // Verification is done in tearDown via wireMock.unmatchedRequests()
  }

  @Test
  void set_negative_internalServerError() {
    addStubMapping("wiremock/stubs/secret-server-issue.json");

    assertThatThrownBy(() -> fsspStore.set(KEY1, VALUE1))
      .isInstanceOf(SecureStoreServiceException.class)
      .hasMessageContaining("Failed to save secret: key = " + KEY1);
  }

  @Test
  void delete_positive() {
    addStubMapping("wiremock/stubs/delete-key1-secret.json");

    fsspStore.delete(KEY1);
    // Verification is done in tearDown via wireMock.unmatchedRequests()
  }

  @Test
  void delete_negative_internalServerError() {
    addStubMapping("wiremock/stubs/secret-server-issue.json");

    assertThatThrownBy(() -> fsspStore.delete(KEY1))
      .isInstanceOf(SecureStoreServiceException.class)
      .hasMessageContaining("Failed to delete secret: key = " + KEY1);
  }

  private static void addStubMapping(String stubJson) {
    var stub = parse(readString(stubJson), StubMapping.class);
    wm.addStubMapping(stub);
  }
}
