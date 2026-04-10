package org.folio.integration.kafka.consumer.filter.mmd.configuration;

import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleData;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleDataProvider;
import org.folio.integration.kafka.consumer.filter.mmd.ModuleMetadata;
import org.folio.integration.kafka.consumer.filter.mmd.impl.AppPropertiesModuleDataProvider;
import org.folio.integration.kafka.consumer.filter.mmd.impl.CompositeModuleDataProvider;
import org.folio.integration.kafka.consumer.filter.mmd.impl.ManifestModuleDataProvider;
import org.folio.integration.kafka.consumer.filter.mmd.impl.ModulePropertiesModuleDataProvider;
import org.folio.integration.kafka.consumer.filter.mmd.impl.PomModuleDataProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ResourceLoader;

@Log4j2
@Configuration
public class ModuleMetadataConfiguration {

  private static final int APP_PROPERTIES_MDP_ORDER = 1;
  private static final int MANIFEST_MDP_ORDER = 10;
  private static final int POM_MDP_ORDER = 20;
  private static final int MODULE_PROPERTIES_MDP_ORDER = 30;

  @Bean
  @Order(APP_PROPERTIES_MDP_ORDER)
  public AppPropertiesModuleDataProvider appPropertiesModuleDataProvider(
    @Value("${spring.application.name}") String applicationName,
    @Value("${spring.application.version}") String applicationVersion) {
    return new AppPropertiesModuleDataProvider(applicationName, applicationVersion);
  }

  @Bean
  @Order(MANIFEST_MDP_ORDER)
  public ManifestModuleDataProvider manifestModuleDataProvider() {
    return new ManifestModuleDataProvider();
  }

  @Bean
  @Order(POM_MDP_ORDER)
  public PomModuleDataProvider pomModuleDataProvider() {
    return new PomModuleDataProvider();
  }

  @Bean
  @Order(MODULE_PROPERTIES_MDP_ORDER)
  public ModulePropertiesModuleDataProvider modulePropertiesModuleDataProvider(ResourceLoader resourceLoader,
    @Value("${spring.application.module-properties.location:classpath:module.properties}")
    String modulePropertiesLocation) {
    return new ModulePropertiesModuleDataProvider(resourceLoader, modulePropertiesLocation);
  }

  @Bean("moduleDataProvider")
  @ConditionalOnMissingBean(name = "moduleDataProvider")
  public ModuleDataProvider moduleDataProvider(List<ModuleDataProvider> providers) {
    return new CompositeModuleDataProvider(providers);
  }

  @Bean
  public ModuleMetadata moduleMetadata(@Qualifier("moduleDataProvider") ModuleDataProvider moduleDataProvider) {
    ModuleData moduleData = moduleDataProvider.getModuleData();
    log.info("Module metadata: name = {}, version = {}", moduleData.name(), moduleData.version());
    return moduleData.asModuleMetadata();
  }
}
