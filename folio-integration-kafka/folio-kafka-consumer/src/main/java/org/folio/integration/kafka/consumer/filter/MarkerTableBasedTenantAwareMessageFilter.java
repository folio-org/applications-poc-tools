package org.folio.integration.kafka.consumer.filter;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.folio.integration.kafka.consumer.KafkaTenantHeaders.FOLIO_TENANT_ID;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.folio.common.utils.OkapiHeaders;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.jspecify.annotations.Nullable;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;

/**
 * Filters Kafka records using the module-version marker stored in each tenant schema.
 *
 * @param <K> Kafka record key type
 * @param <V> Kafka record value type
 */
@Log4j2
public class MarkerTableBasedTenantAwareMessageFilter<K, V> implements RecordFilterStrategy<K, V> {

  private final TenantModuleVersionService tenantModuleVersionService;
  private final FolioModuleMetadata folioModuleMetadata;
  private final boolean enabled;

  /**
   * Creates a marker-table-based tenant filter.
   *
   * @param tenantModuleVersionService resolves the module version recorded for a tenant schema
   * @param folioModuleMetadata provides the tenant schema naming convention
   * @param enabled whether filtering is enabled
   */
  public MarkerTableBasedTenantAwareMessageFilter(TenantModuleVersionService tenantModuleVersionService,
    FolioModuleMetadata folioModuleMetadata, boolean enabled) {
    this.tenantModuleVersionService = tenantModuleVersionService;
    this.folioModuleMetadata = folioModuleMetadata;
    this.enabled = enabled;
    log.info("Kafka marker-table tenant module-version filter {}", enabled ? "enabled" : "disabled");
  }

  @Override
  public boolean filter(ConsumerRecord<K, V> consumerRecord) {
    if (!enabled) {
      return false;
    }

    var tenant = resolveTenant(consumerRecord);
    if (tenant == null) {
      log.warn(
        "Missing or blank tenant header, module-version filter is not applied: topic = {}, key = {}, offset = {}",
        consumerRecord.topic(), consumerRecord.key(), consumerRecord.offset());
      return false;
    }

    var headers = Map.<String, Collection<String>>of(OkapiHeaders.TENANT, List.of(tenant));
    final boolean filtered;
    try (var ignored = new FolioExecutionContextSetter(folioModuleMetadata, headers)) {
      filtered = tenantModuleVersionService.isDifferentModuleVersion();
    }
    log.debug("{} event: tenant = {}, topic = {}, key = {}, offset = {}",
      filtered ? "Skipping" : "Accepting", tenant, consumerRecord.topic(), consumerRecord.key(),
      consumerRecord.offset());
    return filtered;
  }

  static @Nullable String resolveTenant(ConsumerRecord<?, ?> consumerRecord) {
    var tenant = findHeaderValue(consumerRecord, OkapiHeaders.TENANT);
    return tenant != null ? tenant : findHeaderValue(consumerRecord, FOLIO_TENANT_ID);
  }

  private static @Nullable String findHeaderValue(ConsumerRecord<?, ?> consumerRecord, String headerName) {
    for (Header header : consumerRecord.headers()) {
      if (headerName.equalsIgnoreCase(header.key()) && header.value() != null) {
        var value = trimToNull(new String(header.value(), UTF_8));
        if (value != null) {
          return value;
        }
      }
    }
    return null;
  }
}
