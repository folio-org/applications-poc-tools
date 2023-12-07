package org.folio.test.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(KeycloakRealmsGroup.class)
public @interface KeycloakRealms {

  @AliasFor("realms")
  String[] value() default {};

  String[] realms() default {};
}
