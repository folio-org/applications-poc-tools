package org.folio.integration.kafka.consumer.filter.te;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.Set;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "entitlements", contentType = APPLICATION_JSON_VALUE)
public interface TenantEntitlementClient {

  @GetExchange("/modules/{id}")
  Set<String> lookupTenantsByModuleId(@PathVariable("id") String moduleId);
}
