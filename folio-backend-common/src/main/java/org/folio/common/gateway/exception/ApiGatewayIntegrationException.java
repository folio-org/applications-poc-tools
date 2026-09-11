package org.folio.common.gateway.exception;

import java.io.Serial;
import java.util.List;
import lombok.Getter;
import org.folio.common.domain.model.error.Parameter;

/**
 * Base exception for API Gateway integration failures, carrying per-item error parameters.
 */
@Getter
public class ApiGatewayIntegrationException extends RuntimeException {

  @Serial private static final long serialVersionUID = 5285483644401322553L;
  private final transient List<Parameter> errors;

  /**
   * Creates a new {@link ApiGatewayIntegrationException} with corresponding error message.
   *
   * @param message - error message as {@link String} object
   * @param errors - {@link List} with error {@link Parameter} objects
   */
  public ApiGatewayIntegrationException(String message, List<Parameter> errors) {
    super(message);
    this.errors = errors;
  }

  /**
   * Creates a new {@link ApiGatewayIntegrationException} with corresponding error message.
   *
   * @param message - error message as {@link String} object
   * @param errors - {@link List} with error {@link Parameter} objects
   * @param cause - the cause
   */
  public ApiGatewayIntegrationException(String message, List<Parameter> errors, Throwable cause) {
    super(message, cause);
    this.errors = errors;
  }
}
