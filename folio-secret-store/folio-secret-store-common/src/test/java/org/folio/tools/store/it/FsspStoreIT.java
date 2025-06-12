package org.folio.tools.store.it;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.test.TestUtils.readString;
import static org.folio.test.extensions.impl.WireMockExtension.WM_URL_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.test.extensions.LogTestMethod;
import org.folio.test.extensions.impl.WireMockAdminClient;
import org.folio.test.extensions.impl.WireMockExtension;
import org.folio.test.types.IntegrationTest;
import org.folio.tools.store.impl.FsspStore;
import org.folio.tools.store.properties.FsspConfigProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@IntegrationTest
@LogTestMethod
@ExtendWith(WireMockExtension.class)
class FsspStoreIT {

  private static final String KEY1 = "key1";
  private static final String VALUE1 = "value1";

  private static WireMockAdminClient wireMock; // populated by WireMockExtension
  private FsspStore fsspStore;

  @BeforeEach
  void setUp() {
    var properties = FsspConfigProperties.builder()
      .address(System.getProperty(WM_URL_PROPERTY))
      .secretPath("secure-store/entries")
      .enableSsl(false)
      .build();

    fsspStore = new FsspStore(properties);
  }

  @AfterEach
  void tearDown() {
    var unmatched = wireMock.unmatchedRequests().getRequests();

    assertEquals(0, unmatched == null ? 0 : unmatched.size(),
      () -> format("There are unmatched requests to WireMock: %s. Check the mocks are correctly configured",
        unmatched));

    wireMock.resetAll();
  }

  @Test
  void get_positive() {
    wireMock.addStubMapping(readString("wiremock/stubs/get-key1-secret.json"));

    var actual = fsspStore.get(KEY1);
    assertThat(actual).isEqualTo(VALUE1);
  }

  @Test
  void set_positive() {
    wireMock.addStubMapping(readString("wiremock/stubs/put-key1-secret.json"));

    fsspStore.set(KEY1, VALUE1);
    // Verification is done in tearDown via wireMock.unmatchedRequests()
  }

  @Test
  void delete_positive() {
    wireMock.addStubMapping(readString("wiremock/stubs/delete-key1-secret.json"));

    fsspStore.delete(KEY1);
    // Verification is done in tearDown via wireMock.unmatchedRequests()
  }
}
