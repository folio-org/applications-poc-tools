package org.folio.integration.kafka.consumer.recover;

import java.util.function.Function;
import org.folio.integration.kafka.model.ResourceEvent;

public interface ModuleIdExtractor extends Function<ResourceEvent<?>, String> {
}
