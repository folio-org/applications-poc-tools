package org.folio.tools.kong.configuration;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME;

import java.util.Map;
import org.apache.commons.logging.Log;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

@UnitTest
@ExtendWith(MockitoExtension.class)
class DeprecatedKongPropertiesPostProcessorTest {

  private static final String PACKAGED_CONFIG_SOURCE_NAME =
    "Config resource 'class path resource [application.yml]' via location 'optional:classpath:/'";

  @Mock private Log log;

  private DeprecatedKongPropertiesPostProcessor postProcessor;

  @BeforeEach
  void setUp() {
    postProcessor = new DeprecatedKongPropertiesPostProcessor(destination -> log);
  }

  @Test
  void postProcessEnvironment_positive_deprecatedEnvironmentVariables() {
    var environment = environment(Map.of(
      "KONG_INTEGRATION_ENABLED", "true",
      "KONG_ADMIN_URL", "http://kong:8001",
      "REGISTER_MODULE_IN_KONG", "true"));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    verify(log).warn("Environment variable KONG_INTEGRATION_ENABLED is deprecated and will be removed "
      + "in the Vetch release. Use APIGW_ENABLED instead.");
    verify(log).warn("Environment variable KONG_ADMIN_URL is deprecated and will be removed "
      + "in the Vetch release. Use APIGW_URL instead.");
    verify(log).warn("Environment variable REGISTER_MODULE_IN_KONG is deprecated and will be removed "
      + "in the Vetch release. Use APIGW_REGISTER_MODULE instead.");
    verifyNoMoreInteractions(log);
  }

  @Test
  void postProcessEnvironment_positive_deprecatedRelaxedEnvironmentVariable() {
    var environment = environment(Map.of("APPLICATION_KONG_TLS_TRUST_STORE_PATH", "/tmp/truststore.p12"));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    verify(log).warn("Environment variable APPLICATION_KONG_TLS_TRUST_STORE_PATH is deprecated and will be removed "
      + "in the Vetch release. Use APPLICATION_APIGW_TLS_TRUST_STORE_PATH instead.");
    verifyNoMoreInteractions(log);
  }

  @Test
  void postProcessEnvironment_positive_conflictingEnvironmentVariables() {
    var environment = environment(Map.of(
      "KONG_ADMIN_URL", "http://kong:8001",
      "APIGW_URL", "http://apigw:8001"));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    verify(log).warn("Both APIGW_URL and deprecated KONG_ADMIN_URL are set. "
      + "APIGW_URL takes precedence; KONG_ADMIN_URL is ignored.");
    verifyNoMoreInteractions(log);
  }

  @Test
  void postProcessEnvironment_positive_conflictingEnvironmentVariableInRelaxedForm() {
    var environment = environment(Map.of(
      "KONG_TLS_TRUSTSTORE_PATH", "/tmp/legacy.p12",
      "APPLICATION_APIGW_TLS_TRUST_STORE_PATH", "/tmp/truststore.p12"));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    verify(log).warn("Both APIGW_TLS_TRUSTSTORE_PATH and deprecated KONG_TLS_TRUSTSTORE_PATH are set. "
      + "APIGW_TLS_TRUSTSTORE_PATH takes precedence; KONG_TLS_TRUSTSTORE_PATH is ignored.");
    verifyNoMoreInteractions(log);
  }

  @Test
  void postProcessEnvironment_positive_deprecatedProperty() {
    var environment = environment(Map.of(),
      new MapPropertySource("commandLineArgs", Map.of("application.kong.custom.key", "value")));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    verify(log).warn("Configuration property 'application.kong.custom.key' is deprecated and will be removed "
      + "in the Vetch release. Use 'application.apigw.custom.key' instead.");
    verifyNoMoreInteractions(log);
  }

  @Test
  void postProcessEnvironment_positive_conflictingProperties() {
    var environment = environment(Map.of(),
      new MapPropertySource("commandLineArgs", Map.of("application.kong.url", "http://kong:8001")),
      new MapPropertySource("overrides", Map.of("application.apigw.url", "http://apigw:8001")));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    verify(log).warn("Both 'application.apigw.url' and deprecated 'application.kong.url' are set. "
      + "'application.apigw.url' takes precedence; 'application.kong.url' is ignored.");
    verifyNoMoreInteractions(log);
  }

  @Test
  void postProcessEnvironment_positive_packagedClasspathConfigurationIgnored() {
    var environment = environment(Map.of(),
      new MapPropertySource(PACKAGED_CONFIG_SOURCE_NAME, Map.of("application.kong.enabled", "true")));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    verifyNoInteractions(log);
  }

  @Test
  void postProcessEnvironment_positive_kongProductAndModuleUrlVariablesIgnored() {
    var environment = environment(Map.of(
      "KONG_PG_HOST", "postgres",
      "KONG_PROXY_LISTEN", "0.0.0.0:8000",
      "MODULE_URL", "http://mod-test:8081"));

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    verifyNoInteractions(log);
  }

  @Test
  void postProcessEnvironment_positive_noSystemEnvironmentPropertySource() {
    var environment = environment(Map.of());
    environment.getPropertySources().remove(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);

    postProcessor.postProcessEnvironment(environment, new SpringApplication());

    verifyNoInteractions(log);
  }

  private static ConfigurableEnvironment environment(Map<String, Object> systemEnvironment,
    PropertySource<?>... additionalSources) {
    var environment = new StandardEnvironment();
    var propertySources = environment.getPropertySources();
    propertySources.remove(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
    propertySources.replace(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
      new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, systemEnvironment));
    for (var additionalSource : additionalSources) {
      propertySources.addFirst(additionalSource);
    }
    return environment;
  }
}
