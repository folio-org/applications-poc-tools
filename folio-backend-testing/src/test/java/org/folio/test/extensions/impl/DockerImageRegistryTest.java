package org.folio.test.extensions.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class DockerImageRegistryTest {

  @Test
  void getPostgresImageName_positive_returnsDefaultImage() {
    var result = DockerImageRegistry.getPostgresImageName();

    assertThat(result).isEqualTo(DockerImageRegistry.POSTGRES_DEFAULT_IMAGE);
  }

  @Test
  void getWiremockImageName_positive_returnsDefaultImage() {
    var result = DockerImageRegistry.getWiremockImageName();

    assertThat(result).isEqualTo(DockerImageRegistry.WIREMOCK_DEFAULT_IMAGE);
  }

  @Test
  void getKeycloakImageName_positive_returnsDefaultImage() {
    var result = DockerImageRegistry.getKeycloakImageName();

    assertThat(result).isEqualTo(DockerImageRegistry.KEYCLOAK_DEFAULT_IMAGE);
  }

  @Test
  void getKafkaImageName_positive_returnsDefaultImage() {
    var result = DockerImageRegistry.getKafkaImageName();

    assertThat(result).isEqualTo(DockerImageRegistry.KAFKA_DEFAULT_IMAGE);
  }
}
