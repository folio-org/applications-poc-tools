package org.folio.common.service;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionHelper {

  /**
   * Runs {@link Supplier} in the new transaction.
   *
   * @param supplier - value provider as {@link Supplier} function.
   * @param <T> - generic type of supplied value
   * @return supplied value.
   */
  @Transactional
  public <T> T runInTransaction(Supplier<T> supplier) {
    return supplier.get();
  }
}
