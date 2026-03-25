package org.folio.test.extensions.impl;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DockerImageRegistry {

  public static String POSTGRES_IMAGE_ENV = "TESTCONTAINERS_POSTGRES_IMAGE";
  public static String WIREMOCK_IMAGE_ENV = "TESTCONTAINERS_WIREMOCK_IMAGE";
  public static String KEYCLOAK_IMAGE_ENV = "TESTCONTAINERS_KEYCLOAK_IMAGE";
  public static String KAFKA_IMAGE_ENV    = "TESTCONTAINERS_KAFKA_IMAGE";

  public static String POSTGRES_DEFAULT_IMAGE = "postgres:16-alpine";
  public static String WIREMOCK_DEFAULT_IMAGE = "wiremock/3.13.2-2-alpine";
  public static String KEYCLOAK_DEFAULT_IMAGE = "quay.io/keycloak/keycloak:26.5.2";
  public static String KAFKA_DEFAULT_IMAGE    = "apache/kafka-native:3.8.0";

  public static String getPostgresImageName() {
    return getImageName(POSTGRES_IMAGE_ENV, POSTGRES_DEFAULT_IMAGE);
  }

  public static String getWiremockImageName() {
    return getImageName(WIREMOCK_IMAGE_ENV, WIREMOCK_DEFAULT_IMAGE);
  }

  public static String getKeycloakImageName() {
    return getImageName(KEYCLOAK_IMAGE_ENV, KEYCLOAK_DEFAULT_IMAGE);
  }

  public static String getKafkaImageName() {
    return getImageName(KAFKA_IMAGE_ENV, KAFKA_DEFAULT_IMAGE);
  }

  private static String getImageName(String imageEnvVar, String defaultImageName) {
    var env = System.getenv();
    return env.getOrDefault(imageEnvVar, defaultImageName);
  }
}
