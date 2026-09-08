package org.folio.common.gateway.exception;

import java.io.Serial;
import java.util.List;
import org.folio.common.domain.model.error.Parameter;

/**
 * Failure of a tenant add/remove operation on API Gateway routes, carrying per-route error parameters.
 */
public class TenantRouteUpdateException extends ApiGatewayIntegrationException {

  @Serial private static final long serialVersionUID = 7139312177025815438L;

  /**
   * Creates a new {@link TenantRouteUpdateException} with corresponding error message.
   *
   * @param message - error message as {@link String} object
   * @param errors - {@link List} with error {@link Parameter} objects
   */
  public TenantRouteUpdateException(String message, List<Parameter> errors) {
    super(message, errors);
  }

  /**
   * Creates a new {@link TenantRouteUpdateException} with corresponding error message and cause.
   *
   * @param message - error message as {@link String} object
   * @param errors - {@link List} with error {@link Parameter} objects
   * @param cause - the cause
   */
  public TenantRouteUpdateException(String message, List<Parameter> errors, Throwable cause) {
    super(message, errors, cause);
  }
}
