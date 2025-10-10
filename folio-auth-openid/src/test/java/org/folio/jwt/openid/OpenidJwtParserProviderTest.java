package org.folio.jwt.openid;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import io.smallrye.jwt.auth.principal.JWTParser;
import java.util.List;
import java.util.Map;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

@UnitTest
class OpenidJwtParserProviderTest {

  private static final String TENANT_NAME = "test";
  private static final String ISSUER_URI = "https://keycloak:8080/realms/" + TENANT_NAME;

  private OpenidJwtParserProvider openidJwtParserProvider;

  @BeforeEach
  void setUp() {
    openidJwtParserProvider = new OpenidJwtParserProvider(60, 60, null);
  }

  @Test
  void invalidateCache_positive_invalidateAll() {
    var issuerUri = ISSUER_URI;
    var parser = openidJwtParserProvider.getParser(issuerUri);
    assertThat(parser).isNotNull();

    var cachedParser = openidJwtParserProvider.getParser(issuerUri);
    assertThat(cachedParser).isEqualTo(parser);

    var cache = getCache();
    assertThat(cache).containsKey(issuerUri);

    openidJwtParserProvider.invalidateCache();
    assertThat(cache).isEmpty();
  }

  @Test
  void invalidateCache_positive_invalidateByIssuerUri() {
    var issuerUri = ISSUER_URI;
    var parser = openidJwtParserProvider.getParser(issuerUri);
    assertThat(parser).isNotNull();

    var cache = getCache();
    assertThat(cache).containsKey(issuerUri);

    openidJwtParserProvider.invalidateCache(issuerUri);
    assertThat(cache).isEmpty();
  }

  @Test
  void invalidateCache_positive_tenantName() {
    var parser = openidJwtParserProvider.getParser(ISSUER_URI);
    assertThat(parser).isNotNull();

    var cache = getCache();
    assertThat(cache).containsKey(ISSUER_URI);

    openidJwtParserProvider.invalidateCache(List.of(TENANT_NAME));
    assertThat(cache).containsKey(ISSUER_URI);
  }

  @Test
  void invalidateCache_positive_nullCollection() {
    var parser = openidJwtParserProvider.getParser(ISSUER_URI);
    assertThat(parser).isNotNull();

    var cache = getCache();
    assertThat(cache).containsKey(ISSUER_URI);

    openidJwtParserProvider.invalidateCache((List<String>) null);
    assertThat(cache).isEmpty();
  }

  @Test
  void invalidateCache_positive_emptyCollection() {
    var parser = openidJwtParserProvider.getParser(ISSUER_URI);
    assertThat(parser).isNotNull();

    var cache = getCache();
    assertThat(cache).containsKey(ISSUER_URI);

    openidJwtParserProvider.invalidateCache(emptyList());
    assertThat(cache).isEmpty();
  }

  @Test
  void invalidateCache_positive_otherTenantId() {
    var parser = openidJwtParserProvider.getParser(ISSUER_URI);
    assertThat(parser).isNotNull();

    var cache = getCache();
    assertThat(cache).containsKey(ISSUER_URI);

    openidJwtParserProvider.invalidateCache(List.of("other_tenant_id"));
    assertThat(cache).isEmpty();
  }

  @Test
  void getParser_positive_withCustomKeycloakBaseUrl() {
    var customBaseUrl = "http://keycloak-headless:8080";
    var providerWithCustomUrl = new OpenidJwtParserProvider(60, 60, customBaseUrl);

    var parser = providerWithCustomUrl.getParser(ISSUER_URI);
    assertThat(parser).isNotNull();

    var cachedParser = providerWithCustomUrl.getParser(ISSUER_URI);
    assertThat(cachedParser).isEqualTo(parser);
  }

  @Test
  void getParser_positive_withCustomKeycloakBaseUrlTrailingSlash() {
    var customBaseUrl = "http://keycloak-headless:8080/";
    var providerWithCustomUrl = new OpenidJwtParserProvider(60, 60, customBaseUrl);

    var parser = providerWithCustomUrl.getParser(ISSUER_URI);
    assertThat(parser).isNotNull();
  }

  @Test
  void getParser_positive_withEmptyCustomKeycloakBaseUrl() {
    var providerWithEmptyUrl = new OpenidJwtParserProvider(60, 60, "");

    var parser = providerWithEmptyUrl.getParser(ISSUER_URI);
    assertThat(parser).isNotNull();
  }

  @Test
  void getParser_positive_withBlankCustomKeycloakBaseUrl() {
    var providerWithBlankUrl = new OpenidJwtParserProvider(60, 60, "   ");

    var parser = providerWithBlankUrl.getParser(ISSUER_URI);
    assertThat(parser).isNotNull();
  }

  @Test
  void getParser_positive_backwardCompatibility() {
    var providerWithOldConstructor = new OpenidJwtParserProvider(60, 60);

    var parser = providerWithOldConstructor.getParser(ISSUER_URI);
    assertThat(parser).isNotNull();

    var cachedParser = providerWithOldConstructor.getParser(ISSUER_URI);
    assertThat(cachedParser).isEqualTo(parser);
  }

  @SuppressWarnings("unchecked")
  private Map<String, JWTParser> getCache() {
    return (Map<String, JWTParser>) ReflectionTestUtils.getField(openidJwtParserProvider, "tokenParsers");
  }
}
