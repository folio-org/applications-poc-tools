package org.folio.tools.store.utils;

import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

import lombok.experimental.UtilityClass;
import org.apache.commons.text.RandomStringGenerator;

@UtilityClass
public class SecretGenerator {

  private static final RandomStringGenerator RANDOM_STRING_GENERATOR = new RandomStringGenerator.Builder()
    .withinRange('0', 'z')
    .filteredBy(LETTERS, DIGITS)
    .get();

  public static String generateSecret(int length) {
    return RANDOM_STRING_GENERATOR.generate(length);
  }
}
