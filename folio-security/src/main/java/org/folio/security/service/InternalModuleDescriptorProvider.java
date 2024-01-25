package org.folio.security.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.common.domain.model.ModuleDescriptor;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ResourceLoader;

@Log4j2
@RequiredArgsConstructor
public class InternalModuleDescriptorProvider {

  private static final String DESCRIPTOR_PATH = "classpath:descriptors/ModuleDescriptor.json";

  private final ResourceLoader resourceLoader;
  private final ObjectMapper objectMapper;

  private ModuleDescriptor moduleDescriptor;

  @SneakyThrows
  @EventListener(ContextRefreshedEvent.class)
  public void loadModuleDescriptor() {
    log.info("Loading internal module descriptor");
    var descriptorResource = resourceLoader.getResource(DESCRIPTOR_PATH);
    moduleDescriptor = objectMapper.readValue(descriptorResource.getInputStream(), ModuleDescriptor.class);
  }

  public ModuleDescriptor getModuleDescriptor() {
    return moduleDescriptor;
  }
}
