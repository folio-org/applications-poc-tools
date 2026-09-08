package org.folio.integration.kafka.consumer.recover;

import java.util.function.Function;
import org.folio.integration.kafka.model.ResourceEvent;

/**
 * Strategy for extracting a module identifier from a {@link ResourceEvent}.
 *
 * <p>Consumer modules implement this interface as a Spring bean and inject it into
 * {@link ResourceResultEventPublishingRecoverer} so that the correct {@code moduleId}
 * is included in the failure confirmation sent back to the caller.
 *
 * <p>Implementations may return {@code null} when the module identifier is not applicable
 * or cannot be determined; the recoverer passes {@code null} through without error.
 */
public interface ModuleIdExtractor extends Function<ResourceEvent<?>, String> {
}
