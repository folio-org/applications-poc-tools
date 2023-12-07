package org.folio.common.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

@UnitTest
class TransactionHelperTest {

  private final TransactionHelper transactionHelper = new TransactionHelper();

  @Test
  void runInTransaction_positive() {
    var result = transactionHelper.runInTransaction(() -> "value");
    assertThat(result).isEqualTo("value");
  }

  @Test
  void validateThatAllMethodsAreAnnotatedWithTransactional() {
    var methods = transactionHelper.getClass().getDeclaredMethods();
    var annotatedMethodsCount = Arrays.stream(methods)
      .map(method -> method.getAnnotation(Transactional.class))
      .count();

    assertThat(methods).hasSize((int) annotatedMethodsCount);
  }
}
