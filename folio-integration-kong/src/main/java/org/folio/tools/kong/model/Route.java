package org.folio.tools.kong.model;

import static org.springframework.http.HttpStatus.UPGRADE_REQUIRED;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.tools.kong.model.expression.RouteExpression;

@Data
@NoArgsConstructor
public class Route {

  private String id;

  private String name;
  private int priority = 0;
  private Identifier service;

  private String expression;
  private List<String> tags;
  private List<String> protocols;

  @JsonProperty("https_redirect_status_code")
  private int httpsRedirectStatusCode = UPGRADE_REQUIRED.value();

  @JsonProperty("strip_path")
  private boolean stripPath = true;

  @JsonProperty("preserve_host")
  private Boolean preserveHost;

  @JsonProperty("request_buffering")
  private Boolean requestBuffering = true;

  @JsonProperty("response_buffering")
  private Boolean responseBuffering = true;

  @JsonProperty("created_at")
  private Long createdAt;

  @JsonProperty("updated_at")
  private Long updatedAt;

  /**
   * Sets id field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route id(String id) {
    this.id = id;
    return this;
  }

  /**
   * Sets name field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Sets priority field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route priority(int priority) {
    this.priority = priority;
    return this;
  }

  /**
   * Sets service field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route service(Identifier service) {
    this.service = service;
    return this;
  }

  /**
   * Sets expression field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route expression(String expression) {
    this.expression = expression;
    return this;
  }

  /**
   * Sets expression field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route expression(RouteExpression expression) {
    this.expression = expression != null ? expression.toString() : null;
    return this;
  }

  /**
   * Sets tags field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route tags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  /**
   * Sets protocols field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route protocols(List<String> protocols) {
    this.protocols = protocols;
    return this;
  }

  /**
   * Sets httpsRedirectStatusCode field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route httpsRedirectStatusCode(int httpsRedirectStatusCode) {
    this.httpsRedirectStatusCode = httpsRedirectStatusCode;
    return this;
  }

  /**
   * Sets stripPath field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route stripPath(boolean stripPath) {
    this.stripPath = stripPath;
    return this;
  }

  /**
   * Sets preserveHost field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route preserveHost(Boolean preserveHost) {
    this.preserveHost = preserveHost;
    return this;
  }

  /**
   * Sets requestBuffering field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route requestBuffering(Boolean requestBuffering) {
    this.requestBuffering = requestBuffering;
    return this;
  }

  /**
   * Sets responseBuffering field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route responseBuffering(Boolean responseBuffering) {
    this.responseBuffering = responseBuffering;
    return this;
  }

  /**
   * Sets createdAt field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route createdAt(Long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  /**
   * Sets updatedAt field and returns {@link Route}.
   *
   * @return modified {@link Route} value
   */
  public Route updatedAt(Long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
