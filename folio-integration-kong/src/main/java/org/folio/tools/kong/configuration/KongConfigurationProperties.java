package org.folio.tools.kong.configuration;

import lombok.Data;
import org.folio.common.configuration.properties.TlsProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "application.kong")
public class KongConfigurationProperties {

  /**
   * Defines if application manager is integrated with Kong API Gateway.
   */
  private boolean enabled;

  /**
   * Provides Kong API Gateway admin URL.
   */
  private String url;

  /**
   * Module URL for self-registration.
   */
  private String moduleSelfUrl;

  /**
   * Defines if module must be registered.
   */
  private boolean registerModule;

  /**
   * The number of retries to execute upon failure to proxy.
   */
  private Integer retries;

  /**
   * The timeout in milliseconds for establishing a connection from Kong to upstream service.
   */
  private Integer connectTimeout;

  /**
   * The timeout in milliseconds between two successive write operations for transmitting a request from Kong
   * to the upstream service.
   */
  private Integer writeTimeout;

  /**
   * The timeout in milliseconds between two successive read operations for transmitting a request from Kong
   * to the upstream service.
   */
  private Integer readTimeout;

  /**
   * Class that represents TLS connection properties.
   */
  private TlsProperties tls;
}
