package org.folio.integration.kafka.consumer.filter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.Getter;

/**
 * Thrown when a specific tenant is not entitled to receive messages for the current module
 * and the active {@link DisabledTenantStrategy} is {@link DisabledTenantStrategy#FAIL FAIL}.
 */
@Getter
public final class TenantIsDisabledException extends RuntimeException {

  private static final String DEFAULT_MESSAGE_FORMAT =
    "Tenant is not enabled for the module: tenant = %s, module = %s";

  private final String tenant;
  private final String moduleId;

  private TenantIsDisabledException(String tenant, String moduleId) {
    super(String.format(DEFAULT_MESSAGE_FORMAT, tenant, moduleId));
    this.tenant = tenant;
    this.moduleId = moduleId;
  }

  /**
   * Creates a new exception for the given tenant and module.
   *
   * @param tenant   the tenant identifier; must not be blank
   * @param moduleId the module identifier; must not be blank
   * @throws IllegalArgumentException if {@code tenant} or {@code moduleId} is blank
   */
  public static TenantIsDisabledException of(String tenant, String moduleId) {
    if (isBlank(tenant)) {
      throw new IllegalArgumentException("Tenant must not be blank");
    }
    if (isBlank(moduleId)) {
      throw new IllegalArgumentException("Module ID must not be blank");
    }

    return new TenantIsDisabledException(tenant, moduleId);
  }
}
