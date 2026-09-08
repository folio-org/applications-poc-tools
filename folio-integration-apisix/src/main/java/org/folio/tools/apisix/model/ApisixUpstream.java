package org.folio.tools.apisix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@SuppressWarnings("unused")
public class ApisixUpstream {

  private String type;
  private String scheme;
  private Map<String, Integer> nodes;
  private Integer retries;
  private ApisixTimeout timeout;

  @JsonProperty("pass_host")
  private String passHost;

  public ApisixUpstream type(String type) {
    this.type = type;
    return this;
  }

  public ApisixUpstream scheme(String scheme) {
    this.scheme = scheme;
    return this;
  }

  public ApisixUpstream nodes(Map<String, Integer> nodes) {
    this.nodes = nodes;
    return this;
  }

  public ApisixUpstream retries(Integer retries) {
    this.retries = retries;
    return this;
  }

  public ApisixUpstream timeout(ApisixTimeout timeout) {
    this.timeout = timeout;
    return this;
  }

  public ApisixUpstream passHost(String passHost) {
    this.passHost = passHost;
    return this;
  }
}
