package org.folio.common.domain.model.error;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Error {

  private String message;
  private String type;
  private ErrorCode code;
  private List<Parameter> parameters;

  public Error message(String message) {
    this.message = message;
    return this;
  }

  public Error type(String type) {
    this.type = type;
    return this;
  }

  public Error code(ErrorCode code) {
    this.code = code;
    return this;
  }

  public Error parameters(List<Parameter> parameters) {
    this.parameters = parameters;
    return this;
  }

  public Error addParametersItem(Parameter parametersItem) {
    if (this.parameters == null) {
      this.parameters = new ArrayList<>();
    }
    this.parameters.add(parametersItem);
    return this;
  }
}
