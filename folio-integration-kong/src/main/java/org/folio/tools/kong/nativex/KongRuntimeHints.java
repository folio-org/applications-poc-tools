package org.folio.tools.kong.nativex;

import org.folio.tools.kong.client.KongAdminClient;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.DecoratingProxy;

/**
 * GraalVM native-image reachability hints for {@code folio-integration-kong}.
 *
 * <p>Wired via {@code @ImportRuntimeHints} on {@link KongNativeHintsAutoConfiguration} (unconditional, so the
 * hints register during AOT even though {@code KongRegistrarAutoConfiguration} is {@code @ConditionalOnProperty}).</p>
 */
public class KongRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
    // KongAdminClient (@HttpExchange) is created imperatively via buildHttpServiceClient(...) ->
    // HttpServiceProxyFactory.createClient(...), which does not get automatic AOT proxy hints.
    hints.proxies().registerJdkProxy(
        TypeReference.of(KongAdminClient.class),
        TypeReference.of(SpringProxy.class),
        TypeReference.of(Advised.class),
        TypeReference.of(DecoratingProxy.class));
  }
}
