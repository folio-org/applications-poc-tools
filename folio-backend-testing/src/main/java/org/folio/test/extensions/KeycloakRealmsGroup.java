package org.folio.test.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface KeycloakRealmsGroup {

  /**
   * Realms to be imported to keycloak for integration tests.
   *
   * @return - array with {@link KeycloakRealms} annotations.
   */
  KeycloakRealms[] value() default {};
}
