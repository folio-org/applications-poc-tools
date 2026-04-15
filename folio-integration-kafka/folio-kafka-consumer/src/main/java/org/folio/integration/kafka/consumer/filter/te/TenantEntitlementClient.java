package org.folio.integration.kafka.consumer.filter.te;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Set;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Spring HTTP interface client for the tenant-entitlement service.
 *
 * <p>All requests are sent to the {@code /entitlements} resource path relative to the base URL
 * configured via the {@code okapi.url} application property.
 */
@HttpExchange(url = "entitlements", contentType = APPLICATION_JSON_VALUE)
public interface TenantEntitlementClient {

  /**
   * Returns the set of tenant identifiers currently entitled for the given module.
   *
   * @param moduleId the module identifier (e.g. {@code mod-foo-1.0.0})
   * @return set of entitled tenant names; never {@code null}
   */
  @GetExchange("/modules/{id}")
  Set<String> lookupTenantsByModuleId(@PathVariable("id") String moduleId);
}
