package org.folio.test.extensions.impl;

import java.util.Map;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.postgresql.PostgreSQLContainer;

public class PostgresContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String DEFAULT_IMAGE_NAME = "postgres:16-alpine";
  private static final String IMAGE_NAME = getImageName(System.getenv());
  private static final String SPRING_PROPERTY_NAME = "spring.datasource.url";

  @SuppressWarnings("resource")
  private static final PostgreSQLContainer CONTAINER = new PostgreSQLContainer(IMAGE_NAME)
    .withDatabaseName("folio_test")
    .withUsername("folio_admin")
    .withPassword("qwerty123");

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    System.setProperty(SPRING_PROPERTY_NAME, CONTAINER.getJdbcUrl());
  }

  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty(SPRING_PROPERTY_NAME);
  }

  static String getImageName(Map<String, String> env) {
    return env.getOrDefault("TESTCONTAINERS_POSTGRES_IMAGE", DEFAULT_IMAGE_NAME);
  }
}
