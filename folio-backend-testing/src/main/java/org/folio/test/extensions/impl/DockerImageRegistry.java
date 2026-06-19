package org.folio.test.extensions.impl;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DockerImageRegistry {

  public static final String ENV_POSTGRES_IMAGE = "TESTCONTAINERS_POSTGRES_IMAGE";
  public static final String ENV_WIREMOCK_IMAGE = "TESTCONTAINERS_WIREMOCK_IMAGE";
  public static final String ENV_KEYCLOAK_IMAGE = "TESTCONTAINERS_KEYCLOAK_IMAGE";
  public static final String ENV_KAFKA_IMAGE    = "TESTCONTAINERS_KAFKA_IMAGE";

  public static final String POSTGRES_DEFAULT_IMAGE = "postgres:16-alpine";
  public static final String WIREMOCK_DEFAULT_IMAGE = "wiremock/wiremock:3.13.2-2";
  public static final String KEYCLOAK_DEFAULT_IMAGE = "folioci/folio-keycloak:latest";
  public static final String KAFKA_DEFAULT_IMAGE    = "apache/kafka-native:4.2.0";

  public static String getPostgresImageName() {
    return getImageName(ENV_POSTGRES_IMAGE, POSTGRES_DEFAULT_IMAGE);
  }

  public static String getWiremockImageName() {
    return getImageName(ENV_WIREMOCK_IMAGE, WIREMOCK_DEFAULT_IMAGE);
  }

  public static String getKeycloakImageName() {
    return getImageName(ENV_KEYCLOAK_IMAGE, KEYCLOAK_DEFAULT_IMAGE);
  }

  public static String getKafkaImageName() {
    return getImageName(ENV_KAFKA_IMAGE, KAFKA_DEFAULT_IMAGE);
  }

  private static String getImageName(String imageEnvVar, String defaultImageName) {
    var env = System.getenv();
    return env.getOrDefault(imageEnvVar, defaultImageName);
  }
}
