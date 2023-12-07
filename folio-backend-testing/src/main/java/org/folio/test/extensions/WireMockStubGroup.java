package org.folio.test.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface WireMockStubGroup {

  /**
   * Wiremock stubs to be added to wiremock for integration tests.
   *
   * @return - array with {@link WireMockStub} annotations.
   */
  WireMockStub[] value() default {};
}
