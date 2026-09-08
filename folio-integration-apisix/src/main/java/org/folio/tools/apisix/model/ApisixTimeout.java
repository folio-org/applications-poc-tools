package org.folio.tools.apisix.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Upstream timeouts in seconds (decimals allowed by APISIX).
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
@SuppressWarnings("unused")
public class ApisixTimeout {

  private Double connect;
  private Double send;
  private Double read;

  public ApisixTimeout connect(Double connect) {
    this.connect = connect;
    return this;
  }

  public ApisixTimeout send(Double send) {
    this.send = send;
    return this;
  }

  public ApisixTimeout read(Double read) {
    this.read = read;
    return this;
  }
}
