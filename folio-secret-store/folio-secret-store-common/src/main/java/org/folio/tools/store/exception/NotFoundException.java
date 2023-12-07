package org.folio.tools.store.exception;

import java.io.Serial;

public class NotFoundException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1586174011075039404L;

  public NotFoundException(String msg) {
    super(msg);
  }

  public NotFoundException(Throwable t) {
    super(t);
  }
}
