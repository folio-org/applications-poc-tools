package org.folio.common.utils.tls;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.http.HttpClient;
import lombok.experimental.UtilityClass;
import org.folio.common.configuration.properties.TlsProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Utility class for building HTTP Service Clients with TLS support.
 *
 * <p>This class provides methods to create Spring HTTP Service Client proxies
 * with custom TLS/SSL configuration. It replaces the Feign-based approach with
 * Spring's native HTTP Service Clients introduced in Spring Framework 6.1.</p>
 *
 */
@UtilityClass
public class HttpClientTlsUtils {

  /**
   * Build HTTP Service Client with TLS support.
   *
   * <p>Creates an interface-based HTTP client proxy using Spring's HTTP Service Clients.
   * The client is configured with custom TLS/SSL settings if provided.</p>
   *
   * <p><b>TLS Configuration:</b></p>
   * <ul>
   *   <li>If {@code tls} is null or disabled: Uses default HTTP client (no custom TLS)</li>
   *   <li>If {@code tls} is enabled: Configures SSL context with custom truststore</li>
   * </ul>
   *
   * <p><b>Usage Example:</b></p>
   * <pre>{@code
   * @Bean
   * public MyClient myClient(RestClient.Builder restClientBuilder) {
   *   return HttpClientTlsUtils.buildHttpServiceClient(
   *       restClientBuilder,
   *       tlsProperties,
   *       "https://api.example.com",
   *       MyClient.class);
   * }
   * }</pre>
   *
   * @param restClientBuilder Spring's RestClient.Builder (injected from context)
   * @param tls TLS configuration properties (optional, can be null for no custom TLS)
   * @param baseUrl Base URL for the HTTP client
   * @param clientClass Interface class annotated with @HttpExchange
   * @param <T> Type of the client interface
   * @return HTTP Service Client implementation of the interface
   * @throws org.folio.common.utils.exception.SslInitializationException if TLS configuration fails
   */
  public static <T> T buildHttpServiceClient(
      RestClient.Builder restClientBuilder,
      TlsProperties tls,
      String baseUrl,
      Class<T> clientClass) {

    var httpClientBuilder = HttpClient.newBuilder();

    // Configure TLS if enabled with custom truststore
    if (tls != null && tls.isEnabled() && isNotBlank(tls.getTrustStorePath())) {
      httpClientBuilder.sslContext(Utils.buildSslContext(tls));
    }

    var httpClient = httpClientBuilder.build();

    // Build RestClient with TLS-configured HTTP client
    var restClient = restClientBuilder
        .baseUrl(baseUrl)
        .requestFactory(new JdkClientHttpRequestFactory(httpClient))
        .build();

    // Create HTTP Service Client proxy from interface
    var adapter = RestClientAdapter.create(restClient);
    var factory = HttpServiceProxyFactory.builderFor(adapter).build();

    return factory.createClient(clientClass);
  }
}
