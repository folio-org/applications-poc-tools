package org.folio.integration.kafka.consumer.filter;

/** Determines whether a tenant schema has been initialized for a module. */
@FunctionalInterface
public interface TenantInitializationDetector {

  boolean isInitialized(String schema);
}
