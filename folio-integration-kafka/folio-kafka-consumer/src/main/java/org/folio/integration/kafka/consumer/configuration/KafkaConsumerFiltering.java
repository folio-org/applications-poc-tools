package org.folio.integration.kafka.consumer.configuration;

import static org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy.FAIL;
import static org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy.SKIP;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.folio.integration.kafka.consumer.filter.DisabledTenantStrategy;

@Data
public class KafkaConsumerFiltering {

  private TenantFilter tenantFilter = new TenantFilter();

  @Data
  public static class TenantFilter {

    private boolean enabled = false; // should be explicitly enabled
    private boolean ignoreEmptyBatch = true;
    private @NotNull DisabledTenantStrategy tenantDisabledStrategy = SKIP;
    private @NotNull DisabledTenantStrategy allTenantsDisabledStrategy = FAIL;
  }
}
