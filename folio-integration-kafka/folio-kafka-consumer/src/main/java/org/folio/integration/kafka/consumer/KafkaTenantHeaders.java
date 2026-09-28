package org.folio.integration.kafka.consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.common.utils.OkapiHeaders;

/**
 * Utility for building the standard FOLIO tenant Kafka headers.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KafkaTenantHeaders {

  /**
   * Fallback tenant header used by several FOLIO producer utilities (e.g. folio-service-tools'
   * FolioMessageProducer, folio-kafka-wrapper's KafkaProducerRecordBuilder) alongside
   * {@link OkapiHeaders#TENANT}.
   */
  public static final String FOLIO_TENANT_ID = "folio.tenantId";

  /**
   * Builds the standard FOLIO tenant Kafka headers - {@value OkapiHeaders#TENANT} and
   * {@value #FOLIO_TENANT_ID} - both set to the given tenant name.
   *
   * @param tenant - tenant name as {@link String}
   * @return list of {@link Header} objects to attach to a {@link org.apache.kafka.clients.producer.ProducerRecord}
   */
  public static List<Header> tenantHeaders(String tenant) {
    var tenantBytes = tenant.getBytes(UTF_8);
    return List.of(
      new RecordHeader(OkapiHeaders.TENANT, tenantBytes),
      new RecordHeader(FOLIO_TENANT_ID, tenantBytes));
  }
}
