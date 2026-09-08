package org.folio.tools.apisix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@SuppressWarnings("unused")
public class ApisixRoute {

  private String id;
  private String name;
  private String desc;
  private String uri;
  private List<String> methods;
  private List<List<Object>> vars;
  private Integer priority;
  private Integer status;
  private Map<String, String> labels;

  @JsonProperty("service_id")
  private String serviceId;

  public ApisixRoute id(String id) {
    this.id = id;
    return this;
  }

  public ApisixRoute name(String name) {
    this.name = name;
    return this;
  }

  public ApisixRoute desc(String desc) {
    this.desc = desc;
    return this;
  }

  public ApisixRoute uri(String uri) {
    this.uri = uri;
    return this;
  }

  public ApisixRoute methods(List<String> methods) {
    this.methods = methods;
    return this;
  }

  public ApisixRoute vars(List<List<Object>> vars) {
    this.vars = vars;
    return this;
  }

  public ApisixRoute priority(Integer priority) {
    this.priority = priority;
    return this;
  }

  public ApisixRoute status(Integer status) {
    this.status = status;
    return this;
  }

  public ApisixRoute labels(Map<String, String> labels) {
    this.labels = labels;
    return this;
  }

  public ApisixRoute serviceId(String serviceId) {
    this.serviceId = serviceId;
    return this;
  }
}
