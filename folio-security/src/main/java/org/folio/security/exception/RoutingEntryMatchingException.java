package org.folio.security.exception;

public class RoutingEntryMatchingException extends RuntimeException {

  public RoutingEntryMatchingException(String message) {
    super(message);
  }

  /**
   * Creates a new {@link RoutingEntryMatchingException} with message and cause.
   *
   * @param message - error message as {@link String} object
   * @param cause - error cause as {@link Throwable} object
   */
  public RoutingEntryMatchingException(String message, Throwable cause) {
    super(message, cause);
  }
}
