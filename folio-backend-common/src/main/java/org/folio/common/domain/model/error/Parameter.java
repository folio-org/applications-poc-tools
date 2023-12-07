package org.folio.common.domain.model.error;

import lombok.Data;

@Data
public class Parameter {

  private String key;
  private String value;

  public Parameter key(String key) {
    this.key = key;
    return this;
  }

  public Parameter value(String value) {
    this.value = value;
    return this;
  }
}
