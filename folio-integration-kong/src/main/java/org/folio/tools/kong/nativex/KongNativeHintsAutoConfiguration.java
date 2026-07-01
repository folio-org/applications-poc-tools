package org.folio.tools.kong.nativex;

import org.folio.tools.kong.client.KongAdminClient;
import org.folio.tools.kong.model.Identifier;
import org.folio.tools.kong.model.Route;
import org.folio.tools.kong.model.Service;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * Unconditional auto-configuration contributing {@code folio-integration-kong}'s native-image hints during
 * Spring AOT (independent of {@code application.kong.enabled}, which gates the runtime beans).
 *
 * <p>{@link KongRuntimeHints} registers the {@link KongAdminClient} JDK proxy;
 * {@code @RegisterReflectionForBinding} covers the Kong Admin API request/response DTOs
 * ({@link Service}, {@link Route}, {@link Identifier}, and the {@code KongResultList} envelope).</p>
 */
@AutoConfiguration
@ImportRuntimeHints(KongRuntimeHints.class)
@RegisterReflectionForBinding({
  Service.class,
  Route.class,
  Identifier.class,
  KongAdminClient.KongResultList.class
})
public class KongNativeHintsAutoConfiguration {
}
