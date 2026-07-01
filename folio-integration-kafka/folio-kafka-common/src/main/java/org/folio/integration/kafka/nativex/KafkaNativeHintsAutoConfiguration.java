package org.folio.integration.kafka.nativex;

import org.folio.integration.kafka.model.ResourceEvent;
import org.folio.integration.kafka.model.ResourceEventType;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.autoconfigure.AutoConfiguration;

/**
 * Unconditional auto-configuration contributing {@code folio-integration-kafka}'s native-image binding hints.
 *
 * <p>{@link ResourceEvent} is the generic Jackson-bound Kafka event envelope (payload type {@code T} is
 * registered by the consuming application). Its {@code @JsonProperty("new"/"old")} fields and the
 * {@link ResourceEventType} enum must be reflectively reachable for (de)serialization under native image.</p>
 *
 * <p>The {@code TenantEntitlementClient} HTTP interface is registered declaratively via
 * {@code @ImportHttpServices}, so Spring AOT contributes its JDK-proxy hints automatically — no manual proxy
 * hint is needed here.</p>
 */
@AutoConfiguration
@RegisterReflectionForBinding({ResourceEvent.class, ResourceEventType.class})
public class KafkaNativeHintsAutoConfiguration {
}
