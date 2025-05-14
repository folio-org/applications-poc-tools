package org.folio.test.extensions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.sql.Connection;
import java.sql.DriverManager;
import lombok.SneakyThrows;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
@EnablePostgres
class EnablePostgresTest {

  @Test
  @SneakyThrows
  void test() {
    var jdbcUrl = System.getProperty("spring.datasource.url");
    try (Connection connection = DriverManager.getConnection(jdbcUrl, "folio_admin", "qwerty123")) {
      assertThat(connection.getMetaData().getDatabaseMajorVersion(), is(16));
    }
  }
}
