package org.folio.integration.kafka.consumer.filter;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.Getter;

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
