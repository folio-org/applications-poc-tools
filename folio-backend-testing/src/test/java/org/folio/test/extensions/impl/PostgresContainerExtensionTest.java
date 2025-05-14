package org.folio.test.extensions.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class PostgresContainerExtensionTest {

  @Test
  void defaultName() {
    assertThat(PostgresContainerExtension.getImageName(Map.of()), is("postgres:16-alpine"));
  }

  @Test
  void envName() {
    var env = Map.of("FOO", "BAR",
        "TESTCONTAINERS_POSTGRES_IMAGE", "postgres:17-bookworm",
        "BAZ", "FOO");
    assertThat(PostgresContainerExtension.getImageName(env), is("postgres:17-bookworm"));
  }
}
