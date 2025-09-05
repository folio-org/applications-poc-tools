package org.folio.test.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.TestPropertySource;

/**
 * Enables keycloak security.
 */
@Tag("keycloak-security-integration")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@TestPropertySource(properties = {"application.keycloak.enabled=true", "application.security.enabled=true"})
public @interface EnableKeycloakSecurity {}
