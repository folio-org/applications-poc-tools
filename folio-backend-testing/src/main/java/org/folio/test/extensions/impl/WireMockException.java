package org.folio.test.extensions.impl;

public class WireMockException extends RuntimeException {

  public WireMockException(String message) {
    super(message);
  }

  public WireMockException(Throwable cause) {
    super(cause);
  }

  public WireMockException(String message, Throwable cause) {
    super(message, cause);
  }
}
