package org.folio.tools.apisix.exception;

import java.io.Serial;
import java.util.List;
import org.folio.common.domain.model.error.Parameter;
import org.folio.common.gateway.exception.ApiGatewayIntegrationException;

public class ApisixIntegrationException extends ApiGatewayIntegrationException {

  @Serial private static final long serialVersionUID = 4265974924523916122L;

  /**
   * Creates a new {@link ApisixIntegrationException} with corresponding error message.
   *
   * @param message - error message as {@link String} object
   * @param errors - {@link List} with error {@link Parameter} objects
   */
  public ApisixIntegrationException(String message, List<Parameter> errors) {
    super(message, errors);
  }

  /**
   * Creates a new {@link ApisixIntegrationException} with corresponding error message.
   *
   * @param message - error message as {@link String} object
   * @param errors - {@link List} with error {@link Parameter} objects
   * @param cause - the cause
   */
  public ApisixIntegrationException(String message, List<Parameter> errors, Throwable cause) {
    super(message, errors, cause);
  }
}
