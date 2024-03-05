package org.folio.test.extensions;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.test.extensions.impl.KeycloakContainerExtension;
import org.folio.test.extensions.impl.KeycloakExecutionListener;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Starts KeycloakContainerExtension server on a random port in docker and sets all required Spring properties.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(KeycloakContainerExtension.class)
@TestExecutionListeners(value = KeycloakExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
public @interface EnableKeycloak {

  boolean tlsEnabled() default false;
}
