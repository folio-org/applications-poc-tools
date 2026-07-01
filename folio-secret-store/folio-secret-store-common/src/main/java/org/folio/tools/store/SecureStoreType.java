package org.folio.tools.store;

/**
 * Supported secure-store backends, selected at runtime via {@code application.secret-store.type}.
 *
 * <p>Selection is done at runtime (not via build-time {@code @ConditionalOnProperty}) so a single
 * GraalVM native image contains all backends and can be re-pointed by configuration alone.</p>
 */
public enum SecureStoreType {
  EPHEMERAL,
  AWS_SSM,
  VAULT,
  FSSP
}
