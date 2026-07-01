package org.folio.security.nativex;

import org.folio.security.integration.keycloak.client.KeycloakAdminClient;
import org.folio.security.integration.keycloak.client.KeycloakAuthClient;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.DecoratingProxy;

/**
 * GraalVM native-image reachability hints for {@code folio-security}.
 *
 * <p>Wired via {@code @ImportRuntimeHints} on {@link FolioSecurityNativeHintsAutoConfiguration} (an
 * unconditional auto-configuration entry point), so the hints are contributed during Spring AOT regardless of
 * which security/import conditions are active at build time. DTO/token binding reflection is declared with
 * {@code @RegisterReflectionForBinding} on that same class.</p>
 */
public class FolioSecurityRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    // The Keycloak @HttpExchange clients are created imperatively via
    // HttpClientTlsUtils.buildHttpServiceClient(...) -> HttpServiceProxyFactory.createClient(...), which does
    // not get automatic AOT proxy hints. Register the JDK dynamic proxies they run behind.
    for (var client : new Class<?>[] {KeycloakAdminClient.class, KeycloakAuthClient.class}) {
      hints.proxies().registerJdkProxy(
          TypeReference.of(client),
          TypeReference.of(SpringProxy.class),
          TypeReference.of(Advised.class),
          TypeReference.of(DecoratingProxy.class));
    }

    // InternalModuleDescriptorProvider loads the module descriptor from classpath at runtime; native-image
    // does not enumerate the classpath, so the resource must be declared. (ModuleDescriptor itself is
    // registered for binding via @RegisterReflectionForBinding on the wiring auto-configuration.)
    hints.resources().registerPattern("descriptors/ModuleDescriptor.json");
  }
}
