package org.folio.tools.kong.service;

import static java.util.Collections.singletonList;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.tools.kong.configuration.KongConfigurationProperties;
import org.folio.tools.kong.model.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ResourceLoader;

@Log4j2
@RequiredArgsConstructor
public class KongModuleRegistrar {

  private static final String DESCRIPTOR_PATH = "classpath:descriptors/ModuleDescriptor.json";

  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;
  private final KongGatewayService kongGatewayService;
  private final KongConfigurationProperties kongConfigurationProperties;

  @EventListener(ApplicationReadyEvent.class)
  public void registerRoutes() {
    var moduleDescriptor = getModuleDescriptor();
    var moduleId = moduleDescriptor.getId();
    var moduleUrl = kongConfigurationProperties.getModuleSelfUrl();

    log.info("Self-registering service in Kong: moduleId = {}, url = {}", moduleId, moduleUrl);
    kongGatewayService.upsertService(new Service().name(moduleId).url(moduleUrl));
    kongGatewayService.updateRoutes(null, singletonList(moduleDescriptor));
  }

  private ModuleDescriptor getModuleDescriptor() {
    log.info("Loading internal module descriptor");
    var descriptorResource = resourceLoader.getResource(DESCRIPTOR_PATH);
    try {
      return objectMapper.readValue(descriptorResource.getInputStream(), ModuleDescriptor.class);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load module descriptor", e);
    }
  }
}
