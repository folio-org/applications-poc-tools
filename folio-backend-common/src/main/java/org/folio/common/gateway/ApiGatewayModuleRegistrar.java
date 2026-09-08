package org.folio.common.gateway;

import static java.util.Collections.singletonList;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.gateway.model.GatewayServiceDefinition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ResourceLoader;
import tools.jackson.databind.ObjectMapper;

/**
 * Self-registers the hosting module as an API Gateway service with its routes on application startup.
 */
@Log4j2
@RequiredArgsConstructor
public class ApiGatewayModuleRegistrar {

  private static final String DESCRIPTOR_PATH = "classpath:descriptors/ModuleDescriptor.json";

  private final ObjectMapper objectMapper;
  private final ResourceLoader resourceLoader;
  private final ApiGatewayService apiGatewayService;
  private final ModuleRegistrationSettings settings;

  @EventListener(ApplicationReadyEvent.class)
  public void registerRoutes() {
    var moduleDescriptor = getModuleDescriptor();
    var moduleId = moduleDescriptor.getId();
    var moduleUrl = settings.moduleSelfUrl();

    log.info("Self-registering service in API Gateway: moduleId = {}, url = {}", moduleId, moduleUrl);
    apiGatewayService.upsertService(
      new GatewayServiceDefinition().name(moduleId).url(moduleUrl)
        .connectTimeout(settings.connectTimeout())
        .readTimeout(settings.readTimeout())
        .writeTimeout(settings.writeTimeout())
        .retries(settings.retries())
    );
    apiGatewayService.updateRoutes(singletonList(moduleDescriptor));
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
