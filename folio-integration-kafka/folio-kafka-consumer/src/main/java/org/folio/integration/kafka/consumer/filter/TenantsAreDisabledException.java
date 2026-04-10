package org.folio.integration.kafka.consumer.filter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.Getter;

@Getter
public final class TenantsAreDisabledException extends RuntimeException {

  private static final String DEFAULT_MESSAGE_FORMAT = "No tenants are enabled for the module: moduleId = %s";

  private final String moduleId;

  private TenantsAreDisabledException(String moduleId) {
    super(String.format(DEFAULT_MESSAGE_FORMAT, moduleId));
    this.moduleId = moduleId;
  }

  public static TenantsAreDisabledException of(String moduleId) {
    if (isBlank(moduleId)) {
      throw new IllegalArgumentException("Module ID must not be blank");
    }
    return new TenantsAreDisabledException(moduleId);
  }
}
