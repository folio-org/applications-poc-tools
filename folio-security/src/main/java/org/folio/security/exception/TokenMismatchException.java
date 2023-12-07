package org.folio.security.exception;

public class TokenMismatchException extends RuntimeException {

  public TokenMismatchException(String message) {
    super(message);
  }
}
