package org.folio.tools.store.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.tools.store.utils.SecretGenerator.generateSecret;

import org.apache.commons.lang3.StringUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class SecretGeneratorTest {

  private static final int SECRET_LENGTH = 32;

  @Test
  void generateSecret_positive() {
    var secret = generateSecret(SECRET_LENGTH);

    assertThat(secret)
      .isNotBlank()
      .matches(StringUtils::isAlphanumeric)
      .hasSize(SECRET_LENGTH);
  }
}
