package org.folio.tools.kong.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@SuppressWarnings("unused")
public class Service {

  private String id;
  private String name;
  private List<String> tags;
  private String protocol;
  private String host;
  private String url;
  private String path;
  private Integer retries;
  private Integer port = 80;
  private Boolean enabled = true;

  @JsonProperty("connect_timeout")
  private Integer connectTimeout;

  @JsonProperty("write_timeout")
  private Integer writeTimeout;

  @JsonProperty("read_timeout")
  private Integer readTimeout;

  @JsonProperty("client_certificate")
  private Identifier clientCertificate;

  @JsonProperty("tls_verify")
  private Boolean tlsVerify;

  @JsonProperty("tls_verify_depth")
  private Integer tlsVerifyDepth;

  @JsonProperty("created_at")
  private Long createdAt;

  @JsonProperty("updated_at")
  private Long updatedAt;

  public Service id(String id) {
    this.id = id;
    return this;
  }

  public Service name(String name) {
    this.name = name;
    return this;
  }

  public Service tags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public Service protocol(String protocol) {
    this.protocol = protocol;
    return this;
  }

  public Service host(String host) {
    this.host = host;
    return this;
  }

  public Service url(String url) {
    this.url = url;
    return this;
  }

  public Service path(String path) {
    this.path = path;
    return this;
  }

  public Service retries(Integer retries) {
    this.retries = retries;
    return this;
  }

  public Service port(Integer port) {
    this.port = port;
    return this;
  }

  public Service connectTimeout(Integer connectTimeout) {
    this.connectTimeout = connectTimeout;
    return this;
  }

  public Service writeTimeout(Integer writeTimeout) {
    this.writeTimeout = writeTimeout;
    return this;
  }

  public Service readTimeout(Integer readTimeout) {
    this.readTimeout = readTimeout;
    return this;
  }

  public Service clientCertificate(Identifier clientCertificate) {
    this.clientCertificate = clientCertificate;
    return this;
  }

  public Service tlsVerify(Boolean tlsVerify) {
    this.tlsVerify = tlsVerify;
    return this;
  }

  public Service tlsVerifyDepth(Integer tlsVerifyDepth) {
    this.tlsVerifyDepth = tlsVerifyDepth;
    return this;
  }

  public Service createdAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Service updatedAt(Long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Service enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }
}
