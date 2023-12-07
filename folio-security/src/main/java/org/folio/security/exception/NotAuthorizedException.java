package org.folio.security.exception;

public class NotAuthorizedException extends RuntimeException {

  public NotAuthorizedException(String message) {
    super(message);
  }

  public NotAuthorizedException(String message, Throwable cause) {
    super(message, cause);
  }
}
