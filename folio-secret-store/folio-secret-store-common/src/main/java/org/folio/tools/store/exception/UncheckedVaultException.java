package org.folio.tools.store.exception;

import com.bettercloud.vault.VaultException;
import java.io.Serial;

public class UncheckedVaultException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 28912662L;

  public UncheckedVaultException(String msg, VaultException e) {
    super(msg, e);
  }

  public UncheckedVaultException(VaultException e) {
    super(e);
  }
}
