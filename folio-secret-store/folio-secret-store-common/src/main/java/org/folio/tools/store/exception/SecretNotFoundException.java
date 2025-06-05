package org.folio.tools.store.exception;

import java.io.Serial;

public class SecretNotFoundException extends SecureStoreServiceException {

  @Serial
  private static final long serialVersionUID = 1586174011075039404L;

  public SecretNotFoundException(String msg) {
    super(msg);
  }
}
