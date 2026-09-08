package org.folio.common.gateway.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Gateway-neutral definition of an API Gateway service (upstream) for a FOLIO module.
 */
@Data
@NoArgsConstructor
public class GatewayServiceDefinition {

  private String name;
  private String url;
  private Integer retries;
  private Integer connectTimeout;
  private Integer readTimeout;
  private Integer writeTimeout;

  public GatewayServiceDefinition name(String name) {
    this.name = name;
    return this;
  }

  public GatewayServiceDefinition url(String url) {
    this.url = url;
    return this;
  }

  public GatewayServiceDefinition retries(Integer retries) {
    this.retries = retries;
    return this;
  }

  public GatewayServiceDefinition connectTimeout(Integer connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  public GatewayServiceDefinition readTimeout(Integer readTimeout) {
    this.readTimeout = readTimeout;
    return this;
  }

  public GatewayServiceDefinition writeTimeout(Integer writeTimeout) {
    this.writeTimeout = writeTimeout;
    return this;
  }
}
