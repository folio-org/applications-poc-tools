package org.folio.tools.store.exception;

import java.io.Serial;

public class SecureStoreServiceException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 3917158805645543244L;

  public SecureStoreServiceException(String message) {
    super(message);
  }

  public SecureStoreServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public SecureStoreServiceException(Throwable cause) {
    super(cause);
  }
}
