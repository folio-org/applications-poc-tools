package org.folio.test.extensions;

import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.folio.test.extensions.impl.WireMockExecutionListener;
import org.folio.test.extensions.impl.WireMockExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestExecutionListeners;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(WireMockExtension.class)
@TestExecutionListeners(value = WireMockExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
public @interface EnableWireMock {}
