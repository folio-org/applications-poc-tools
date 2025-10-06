package org.folio.tools.kong.exception;

import java.io.Serial;

/**
 * Exception thrown when tenant route update operations fail.
 */
public class TenantRouteUpdateException extends RuntimeException {

  @Serial private static final long serialVersionUID = 7234567890123456789L;

  /**
   * Creates a new {@link TenantRouteUpdateException} with corresponding error message.
   *
   * @param message - error message as {@link String} object
   */
  public TenantRouteUpdateException(String message) {
    super(message);
  }

  /**
   * Creates a new {@link TenantRouteUpdateException} with corresponding error message and cause.
   *
   * @param message - error message as {@link String} object
   * @param cause - the cause
   */
  public TenantRouteUpdateException(String message, Throwable cause) {
    super(message, cause);
  }
}
