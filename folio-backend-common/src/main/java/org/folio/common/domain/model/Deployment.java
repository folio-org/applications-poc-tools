package org.folio.common.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class Deployment {

  private String type;
  private String chart;

  public Deployment type(String type) {
    this.type = type;
    return this;
  }

  public Deployment chart(String chart) {
    this.chart = chart;
    return this;
  }
}
