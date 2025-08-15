package org.folio.test.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

/**
 * Enables keycloak data import.
 */
@Tag("keycloak-import-integration")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@TestPropertySource(properties = {
  "application.keycloak.enabled=true",
  "application.keycloak.import.enabled=true",
  "application.secure-store.environment=folio",
  })
public @interface EnableKeycloakDataImport {}
