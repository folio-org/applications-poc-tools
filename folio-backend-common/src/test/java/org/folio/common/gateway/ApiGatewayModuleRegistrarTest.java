package org.folio.common.gateway;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.common.gateway.model.GatewayServiceDefinition;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import tools.jackson.databind.ObjectMapper;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ApiGatewayModuleRegistrarTest {

  private static final String DESCRIPTOR_PATH = "classpath:descriptors/ModuleDescriptor.json";
  private static final String MODULE_URL = "https://test-module:8081";
  private static final String MODULE_ID = "test-service";

  @Mock private Resource resource;
  @Mock private InputStream inputStream;
  @Mock private ModuleDescriptor moduleDescriptor;
  @Mock private ObjectMapper objectMapper;
  @Mock private ResourceLoader resourceLoader;
  @Mock private ApiGatewayService apiGatewayService;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(objectMapper, resourceLoader, apiGatewayService);
  }

  @Test
  void registerRoutes_positive() throws Exception {
    var settings = new ModuleRegistrationSettings(MODULE_URL, 5, 60000, 60000, 60000);
    when(resourceLoader.getResource(DESCRIPTOR_PATH)).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(inputStream);
    when(objectMapper.readValue(inputStream, ModuleDescriptor.class)).thenReturn(moduleDescriptor);
    when(moduleDescriptor.getId()).thenReturn(MODULE_ID);

    var registrar = new ApiGatewayModuleRegistrar(objectMapper, resourceLoader, apiGatewayService, settings);
    registrar.registerRoutes();

    var expectedService = new GatewayServiceDefinition().name(MODULE_ID).url(MODULE_URL)
      .connectTimeout(60000)
      .writeTimeout(60000)
      .readTimeout(60000)
      .retries(5);
    verify(apiGatewayService).upsertService(expectedService);
    verify(apiGatewayService).updateRoutes(List.of(moduleDescriptor));
  }

  @Test
  void registerRoutes_negative_failedToLoadModuleDescriptor() throws Exception {
    var settings = new ModuleRegistrationSettings(MODULE_URL, null, null, null, null);
    var registrar = new ApiGatewayModuleRegistrar(objectMapper, resourceLoader, apiGatewayService, settings);
    when(resourceLoader.getResource(DESCRIPTOR_PATH)).thenReturn(resource);
    when(resource.getInputStream()).thenThrow(IOException.class);

    assertThatThrownBy(registrar::registerRoutes)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to load module descriptor");

    verify(apiGatewayService, never()).upsertService(any());
    verify(apiGatewayService, never()).updateRoutes(any());
  }
}
