package org.folio.integration.kafka.consumer.filter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.Getter;

/**
 * Thrown when no tenant is currently entitled to receive messages for the current module
 * and the active {@link DisabledTenantStrategy} is {@link DisabledTenantStrategy#FAIL FAIL}.
 */
@Getter
public final class TenantsAreDisabledException extends RuntimeException {

  private static final String DEFAULT_MESSAGE_FORMAT = "No tenants are enabled for the module: moduleId = %s";

  private final String moduleId;

  private TenantsAreDisabledException(String moduleId) {
    super(String.format(DEFAULT_MESSAGE_FORMAT, moduleId));
    this.moduleId = moduleId;
  }

  /**
   * Creates a new exception for the given module.
   *
   * @param moduleId the module identifier; must not be blank
   * @throws IllegalArgumentException if {@code moduleId} is blank
   */
  public static TenantsAreDisabledException of(String moduleId) {
    if (isBlank(moduleId)) {
      throw new IllegalArgumentException("Module ID must not be blank");
    }
    return new TenantsAreDisabledException(moduleId);
  }
}
