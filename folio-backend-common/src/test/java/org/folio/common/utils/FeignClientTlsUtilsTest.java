package org.folio.common.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.FeignClientTlsUtils.buildTargetFeignClient;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import feign.Client;
import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.apache.http.ssl.SSLInitializationException;
import org.folio.common.configuration.properties.TlsProperties;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class FeignClientTlsUtilsTest {

  private final OkHttpClient okHttpClient = mock(OkHttpClient.class);

  @Test
  void buildTargetFeignClient_negative_invalidTruststorePath() {
    TlsProperties tls = TlsProperties.of(true, "dummy", "dummy");
    assertThrows(SSLInitializationException.class, () -> buildTargetFeignClient(okHttpClient, mock(Contract.class),
      mock(Encoder.class), mock(Decoder.class), tls, "dummy", Client.class));
  }

  @Test
  void buildTargetFeignClient_positive_tlsDisabled() {
    Client client = buildTargetFeignClient(okHttpClient, mock(Contract.class), mock(Encoder.class),
      mock(Decoder.class), TlsProperties.of(false, null, null), "dummy", Client.class);
    assertThat(client).isNotNull();
  }

  @Test
  void buildTargetFeignClient_positive_tlsEnabled() {
    when(okHttpClient.newBuilder()).thenReturn(new Builder());

    Client client = buildTargetFeignClient(okHttpClient, mock(Contract.class), mock(Encoder.class),
      mock(Decoder.class), getEnabledTlsProperties(), "dummy", Client.class);
    assertThat(client).isNotNull();
  }

  @Test
  void buildTargetFeignClient_positive_tlsEnabledWithDefaultTruststore() {
    when(okHttpClient.newBuilder()).thenReturn(new Builder());

    Client client = buildTargetFeignClient(okHttpClient, mock(Contract.class), mock(Encoder.class),
      mock(Decoder.class), TlsProperties.of(true, "", ""), "dummy", Client.class);
    assertThat(client).isNotNull();
  }

  private static TlsProperties getEnabledTlsProperties() {
    return TlsProperties.of(true, "classpath:certificates/test.truststore.jks", "secretpassword");
  }
}
