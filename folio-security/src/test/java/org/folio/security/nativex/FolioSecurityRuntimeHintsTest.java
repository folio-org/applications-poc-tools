package org.folio.security.nativex;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.security.integration.keycloak.client.KeycloakAdminClient;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.core.DecoratingProxy;

@UnitTest
class FolioSecurityRuntimeHintsTest {

  private final RuntimeHints hints = new RuntimeHints();

  FolioSecurityRuntimeHintsTest() {
    new FolioSecurityRuntimeHints().registerHints(hints, getClass().getClassLoader());
  }

  @Test
  void registerHints_positive_registersKeycloakAdminClientJdkProxy() {
    assertThat(RuntimeHintsPredicates.proxies().forInterfaces(
      KeycloakAdminClient.class, SpringProxy.class, Advised.class, DecoratingProxy.class)).accepts(hints);
  }

  @Test
  void registerHints_positive_registersKeycloakAuthClientJdkProxy() {
    assertThat(RuntimeHintsPredicates.proxies().forInterfaces(
      KeycloakAuthClient.class, SpringProxy.class, Advised.class, DecoratingProxy.class)).accepts(hints);
  }

  @Test
  void registerHints_positive_registersModuleDescriptorResource() {
    assertThat(RuntimeHintsPredicates.resource().forResource("descriptors/ModuleDescriptor.json")).accepts(hints);
  }
}
