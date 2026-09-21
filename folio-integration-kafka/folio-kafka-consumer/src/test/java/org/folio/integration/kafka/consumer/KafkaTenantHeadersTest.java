package org.folio.integration.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.folio.integration.kafka.consumer.KafkaTenantHeaders.FOLIO_TENANT_ID;

import java.nio.charset.StandardCharsets;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class KafkaTenantHeadersTest {

  @Test
  void tenantHeaders_positive() {
    var tenantBytes = "test-tenant".getBytes(StandardCharsets.UTF_8);

    var headers = KafkaTenantHeaders.tenantHeaders("test-tenant");

    assertThat(headers).hasSize(2);
    assertThat(headers.get(0).key()).isEqualTo(TENANT);
    assertThat(headers.get(0).value()).isEqualTo(tenantBytes);
    assertThat(headers.get(1).key()).isEqualTo(FOLIO_TENANT_ID);
    assertThat(headers.get(1).value()).isEqualTo(tenantBytes);
  }
}
