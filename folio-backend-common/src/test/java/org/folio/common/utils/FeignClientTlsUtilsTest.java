package org.folio.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.FeignClientTlsUtils.buildSslContext;
import static org.folio.common.utils.FeignClientTlsUtils.buildTargetFeignClient;
import static org.folio.common.utils.FeignClientTlsUtils.getHttpClientBuilder;
import static org.folio.common.utils.FeignClientTlsUtils.getSslOkHttpClient;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.Client;
import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.common.utils.exception.SslInitializationException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class FeignClientTlsUtilsTest {

  private final OkHttpClient okHttpClient = mock(OkHttpClient.class);
  private final Contract contract = mock(Contract.class);
  private final Encoder encoder = mock(Encoder.class);
  private final Decoder decoder = mock(Decoder.class);

  @Test
  void buildTargetFeignClient_positive_tlsDisabled() {
    var client = buildTargetFeignClient(okHttpClient, contract, encoder, decoder,
      TlsProperties.of(false, null, null, null), "dummy", Client.class);
    assertThat(client).isNotNull();
  }

  @Test
  void buildTargetFeignClient_positive_tlsEnabled() {
    when(okHttpClient.newBuilder()).thenReturn(new Builder());

    var client = buildTargetFeignClient(okHttpClient, contract, encoder, decoder, getEnabledTlsProperties(), "dummy",
      Client.class);
    assertThat(client).isNotNull();
  }

  @Test
  void buildTargetFeignClient_positive_tlsEnabledWithDefaultTruststore() {
    when(okHttpClient.newBuilder()).thenReturn(new Builder());

    var client = buildTargetFeignClient(okHttpClient, contract, encoder, decoder,
      TlsProperties.of(true, "", null, null), "dummy", Client.class);
    assertThat(client).isNotNull();
  }

  @Test
  void getSslOkHttpClient_negative() {
    assertThrows(SslInitializationException.class, () -> getSslOkHttpClient(okHttpClient, getInvalidTlsProperties()));
  }

  @Test
  void buildSslContext_negative_invalidTruststorePath() {
    assertThrows(SslInitializationException.class, () -> buildSslContext(getInvalidTlsProperties()));
  }

  @Test
  void buildSslContext_positive() {
    var sslContext = buildSslContext(getEnabledTlsProperties());
    assertThat(sslContext).isNotNull();
  }

  @Test
  void buildTargetHttpClient_positive_tlsDisabled() {
    var client = getHttpClientBuilder(
      TlsProperties.of(false, null, null, null));
    assertThat(client).isNotNull();
  }

  @Test
  void buildTargetHttpClient_positive_tlsEnabledWithPublicCa() {
    var client = getHttpClientBuilder(
      TlsProperties.of(true, null, null, null));
    assertThat(client).isNotNull();
  }

  @Test
  void buildTargetHttpClient_positive_tlsEnabled() {
    var client = getHttpClientBuilder(getEnabledTlsProperties());
    assertThat(client).isNotNull();
  }

  @Test
  void buildTargetHttpClient_positive_negative() {
    assertThrows(SslInitializationException.class, () -> getHttpClientBuilder(getInvalidTlsProperties()));
  }

  private static TlsProperties getEnabledTlsProperties() {
    return TlsProperties.of(true, "classpath:certificates/test.truststore.jks", "secretpassword", "JKS");
  }

  private static TlsProperties getInvalidTlsProperties() {
    return TlsProperties.of(true, "dummy", "dummy", "");
  }
}
