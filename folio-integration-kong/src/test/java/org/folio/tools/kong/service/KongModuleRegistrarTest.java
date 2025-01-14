package org.folio.tools.kong.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.folio.common.domain.model.ModuleDescriptor;
import org.folio.test.types.UnitTest;
import org.folio.tools.kong.configuration.KongConfigurationProperties;
import org.folio.tools.kong.model.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KongModuleRegistrarTest {

  @InjectMocks private KongModuleRegistrar kongModuleRegistrar;
  @Mock private Resource resource;
  @Mock private InputStream inputStream;
  @Mock private ModuleDescriptor moduleDescriptor;

  @Mock private ObjectMapper objectMapper;
  @Mock private ResourceLoader resourceLoader;
  @Mock private KongGatewayService kongGatewayService;
  @Mock private KongConfigurationProperties kongConfigurationProperties;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(objectMapper, resourceLoader, kongGatewayService);
  }

  @Test
  void registerRoutes_positive() throws Exception {
    var path = "classpath:descriptors/ModuleDescriptor.json";
    var moduleUrl = "https://test-module:8081";
    var serviceName = "test-service";
    var connectTimeout = 60000;
    var readTimeout = 60000;
    var writeTimeout = 60000;
    var retries = 5;

    when(kongConfigurationProperties.getModuleSelfUrl()).thenReturn(moduleUrl);
    when(kongConfigurationProperties.getConnectTimeout()).thenReturn(connectTimeout);
    when(kongConfigurationProperties.getReadTimeout()).thenReturn(readTimeout);
    when(kongConfigurationProperties.getWriteTimeout()).thenReturn(writeTimeout);
    when(kongConfigurationProperties.getRetries()).thenReturn(retries);

    when(resourceLoader.getResource(path)).thenReturn(resource);
    when(resource.getInputStream()).thenReturn(inputStream);
    when(objectMapper.readValue(inputStream, ModuleDescriptor.class)).thenReturn(moduleDescriptor);
    when(moduleDescriptor.getId()).thenReturn(serviceName);

    kongModuleRegistrar.registerRoutes();

    var expectedService = new Service().name(serviceName).url(moduleUrl)
      .connectTimeout(connectTimeout)
      .writeTimeout(writeTimeout)
      .readTimeout(readTimeout)
      .retries(retries);
    verify(kongGatewayService).upsertService(expectedService);
    verify(kongGatewayService).updateRoutes(List.of(moduleDescriptor));
  }

  @Test
  void registerRoutes_negative_failedToLoadModuleDescriptor() throws Exception {
    var path = "classpath:descriptors/ModuleDescriptor.json";

    when(resourceLoader.getResource(path)).thenReturn(resource);
    when(resource.getInputStream()).thenThrow(IOException.class);

    assertThatThrownBy(() -> kongModuleRegistrar.registerRoutes())
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Failed to load module descriptor");

    verify(kongGatewayService, never()).upsertService(any());
    verify(kongGatewayService, never()).updateRoutes(any());
  }
}
