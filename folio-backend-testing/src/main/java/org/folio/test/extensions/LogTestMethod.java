package org.folio.test.extensions;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.test.extensions.impl.LogTestMethodExecutionListener;
import org.springframework.test.context.TestExecutionListeners;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestExecutionListeners(value = {LogTestMethodExecutionListener.class}, mergeMode = MERGE_WITH_DEFAULTS)
public @interface LogTestMethod {
}
