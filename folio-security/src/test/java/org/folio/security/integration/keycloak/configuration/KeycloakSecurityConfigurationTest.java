package org.folio.security.integration.keycloak.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import javax.net.ssl.SSLContext;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.common.utils.exception.SslInitializationException;
import org.folio.jwt.openid.JsonWebTokenParser;
import org.folio.jwt.openid.OpenidJwtParserProvider;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.security.integration.keycloak.configuration.properties.KeycloakProperties;
import org.folio.security.integration.keycloak.service.KeycloakAuthorizationService;
import org.folio.security.integration.keycloak.service.KeycloakPublicKeyProvider;
import org.folio.security.service.InternalModuleDescriptorProvider;
import org.folio.security.service.RoutingEntryMatcher;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UrlPathHelper;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakSecurityConfigurationTest {

  @InjectMocks private KeycloakSecurityConfiguration configuration;

  @Mock private KeycloakProperties properties;
  @Mock private KeycloakAuthClient keycloakAuthClient;
  @Mock private JsonWebTokenParser jsonWebTokenParser;
  @Mock private Environment environment;
  @Mock private UrlPathHelper urlPathHelper;
  @Mock private ObjectMapper objectMapper;
  @Mock private OpenidJwtParserProvider openidJwtParserProvider;
  @Mock private InternalModuleDescriptorProvider descriptorProvider;
  @Mock private ResourceLoader resourceLoader;

  @Test
  void keycloakAuthClient_positive() {
    when(properties.getTls()).thenReturn(null);
    when(properties.getUrl()).thenReturn("http://localhost:8080");

    var result = configuration.keycloakAuthClient();

    assertThat(result).isNotNull();
  }

  @Test
  void authorizationService_positive() {
    var result = configuration.authorizationService(keycloakAuthClient, new RoutingEntryMatcher(descriptorProvider),
      jsonWebTokenParser, environment, urlPathHelper);

    assertThat(result).isNotNull().isInstanceOf(KeycloakAuthorizationService.class);
  }

  @Test
  void publicKeyProvider_positive() {
    var result = configuration.publicKeyProvider(keycloakAuthClient);

    assertThat(result).isNotNull().isInstanceOf(KeycloakPublicKeyProvider.class);
  }

  @Test
  void jsonWebTokenParser_positive() {
    when(properties.getJwtCacheConfiguration()).thenReturn(new KeycloakProperties.KeycloakJwtCacheProperties());
    when(properties.getUrl()).thenReturn("http://localhost:8080");

    var result = configuration.jsonWebTokenParser(objectMapper, properties, openidJwtParserProvider);

    assertThat(result).isNotNull().isInstanceOf(JsonWebTokenParser.class);
  }

  @Test
  void openidJwtParserProvider_positive() {
    when(properties.getJwtCacheConfiguration()).thenReturn(new KeycloakProperties.KeycloakJwtCacheProperties());
    when(properties.getJwksBaseUrl()).thenReturn("http://localhost:8080");

    var result = configuration.openidJwtParserProvider();

    assertThat(result).isNotNull().isInstanceOf(OpenidJwtParserProvider.class);
    assertThat(sslContext(result)).isNull();
  }

  @Test
  void openidJwtParserProvider_positive_tlsEnabledWithTruststore() {
    mockJwtParserProperties();
    when(properties.getTls()).thenReturn(tls(true, "classpath:certificates/test.truststore.jks"));

    var result = configuration.openidJwtParserProvider();

    assertThat(sslContext(result)).isNotNull();
  }

  @Test
  void openidJwtParserProvider_positive_tlsDisabled() {
    mockJwtParserProperties();
    when(properties.getTls()).thenReturn(tls(false, "classpath:certificates/test.truststore.jks"));

    var result = configuration.openidJwtParserProvider();

    assertThat(sslContext(result)).isNull();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = " ")
  void openidJwtParserProvider_positive_tlsEnabledWithoutTruststorePath(String trustStorePath) {
    mockJwtParserProperties();
    when(properties.getTls()).thenReturn(tls(true, trustStorePath));

    var result = configuration.openidJwtParserProvider();

    assertThat(sslContext(result)).isNull();
  }

  @Test
  void openidJwtParserProvider_negative_tlsEnabledWithMissingTruststore() {
    mockJwtParserProperties();
    when(properties.getTls()).thenReturn(tls(true, "classpath:certificates/missing.truststore.jks"));

    assertThatThrownBy(() -> configuration.openidJwtParserProvider())
      .isInstanceOf(SslInitializationException.class);
  }

  @Test
  void routingEntryMatcher_positive() {
    var result = configuration.routingEntryMatcher(descriptorProvider);

    assertThat(result).isNotNull().isInstanceOf(RoutingEntryMatcher.class);
  }

  @Test
  void moduleDescriptorProvider_positive() {
    var result = configuration.moduleDescriptorProvider(resourceLoader, objectMapper);

    assertThat(result).isNotNull().isInstanceOf(InternalModuleDescriptorProvider.class);
  }

  private void mockJwtParserProperties() {
    when(properties.getJwtCacheConfiguration()).thenReturn(new KeycloakProperties.KeycloakJwtCacheProperties());
    when(properties.getJwksBaseUrl()).thenReturn("https://localhost:8443");
  }

  private static TlsProperties tls(boolean enabled, String trustStorePath) {
    var tls = new TlsProperties();
    tls.setEnabled(enabled);
    tls.setTrustStorePath(trustStorePath);
    tls.setTrustStorePassword("secretpassword");
    tls.setTrustStoreType("JKS");
    return tls;
  }

  private static SSLContext sslContext(OpenidJwtParserProvider provider) {
    return (SSLContext) ReflectionTestUtils.getField(provider, "sslContext");
  }
}
