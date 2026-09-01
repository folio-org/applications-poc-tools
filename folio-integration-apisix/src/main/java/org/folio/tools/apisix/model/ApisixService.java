package org.folio.tools.apisix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@SuppressWarnings("unused")
public class ApisixService {

  private String id;
  private String name;
  private String desc;
  private Map<String, String> labels;
  private Map<String, Object> plugins;
  private ApisixUpstream upstream;

  public ApisixService id(String id) {
    this.id = id;
    return this;
  }

  public ApisixService name(String name) {
    this.name = name;
    return this;
  }

  public ApisixService desc(String desc) {
    this.desc = desc;
    return this;
  }

  public ApisixService labels(Map<String, String> labels) {
    this.labels = labels;
    return this;
  }

  public ApisixService plugins(Map<String, Object> plugins) {
    this.plugins = plugins;
    return this;
  }

  public ApisixService upstream(ApisixUpstream upstream) {
    this.upstream = upstream;
    return this;
  }
}
