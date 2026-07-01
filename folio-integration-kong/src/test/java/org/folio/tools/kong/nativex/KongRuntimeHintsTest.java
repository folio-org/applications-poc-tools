package org.folio.tools.kong.nativex;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.folio.tools.kong.client.KongAdminClient;
import org.junit.jupiter.api.Test;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.DecoratingProxy;

@UnitTest
class KongRuntimeHintsTest {

  private final RuntimeHints hints = new RuntimeHints();

  KongRuntimeHintsTest() {
    new KongRuntimeHints().registerHints(hints, getClass().getClassLoader());
  }

  @Test
  void registerHints_positive_registersKongAdminClientJdkProxy() {
    assertThat(RuntimeHintsPredicates.proxies().forInterfaces(
      KongAdminClient.class, SpringProxy.class, Advised.class, DecoratingProxy.class)).accepts(hints);
  }
}
