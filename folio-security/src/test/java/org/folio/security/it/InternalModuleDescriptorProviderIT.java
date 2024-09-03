package org.folio.security.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.folio.security.service.InternalModuleDescriptorProvider;
import org.folio.security.support.TestConfiguration;
import org.folio.test.types.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@IntegrationTest
@SpringBootTest(classes = {InternalModuleDescriptorProvider.class, TestConfiguration.class})
class InternalModuleDescriptorProviderIT {

  @Autowired private InternalModuleDescriptorProvider provider;

  @Test
  void verifyModuleDescriptorLoadedOnAppStartup() {
    var descriptor = provider.getModuleDescriptor();
    assertNotNull(descriptor);
    assertEquals("Test Manager Component", descriptor.getDescription());
  }
}
