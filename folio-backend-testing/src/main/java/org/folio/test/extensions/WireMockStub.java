package org.folio.test.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(WireMockStubGroup.class)
public @interface WireMockStub {

  /**
   * Alias for {@link #scripts}.
   *
   * <p>This attribute may <strong>not</strong> be used in conjunction with
   * {@link #scripts}, but it may be used instead of {@link #scripts}.
   *
   * @see #scripts
   */
  @AliasFor("scripts")
  String[] value() default {};

  /**
   * A array of paths for wiremock json stubs.
   *
   * <p>This attribute may <strong>not</strong> be used in conjunction with
   * {@link #value}, but it may be used instead of {@link #value}.</p> <br/>
   * <h3>Path Resource Semantics</h3>
   * <p>Each path will be interpreted as a Spring</p>
   * {@link org.springframework.core.io.Resource Resource}. A plain path &mdash; for example, {@code "stub.json"}
   * &mdash; will be treated as a classpath resource that is <em>relative</em> to the package in which the test class is
   * defined. A path starting with a slash will be treated as an
   * <em>absolute</em> classpath resource, for example:
   * {@code "/org/example/stub.json"}. A path which references a URL (e.g., a path prefixed with
   * {@link org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
   * {@link org.springframework.util.ResourceUtils#FILE_URL_PREFIX file:}, {@code http:}, etc.) will be loaded using the
   * specified resource protocol.
   */
  String[] scripts() default {};
}
