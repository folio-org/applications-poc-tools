package org.folio.test.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.TestPropertySource;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@TestPropertySource(properties = {
  "application.okapi.enabled=true",
  "application.keycloak.enabled=false",
  "application.security.enabled=true"})
public @interface EnableOkapiSecurity {
}
