package org.folio.tools.kong.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class ApiGatewayAutoConfigurationTest {

  private static final String ADMIN_CLIENT_BEAN = "folioKongAdminClient";
  private static final String GATEWAY_SERVICE_BEAN = "folioKongGatewayService";
  private static final String ROUTE_TENANT_SERVICE_BEAN = "folioKongRouteTenantService";
  private static final String MODULE_REGISTRAR_BEAN = "folioApiGatewayModuleRegistrar";

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(ApiGatewayAutoConfiguration.class))
    .withBean(JsonMapper.class, JsonMapper::new);

  @Test
  void autoConfiguration_positive_enabled() {
    contextRunner
      .withPropertyValues("application.apigw.enabled=true", "application.apigw.url=http://localhost:8001")
      .run(context -> assertThat(context)
        .hasBean(ADMIN_CLIENT_BEAN)
        .hasBean(GATEWAY_SERVICE_BEAN)
        .hasBean(ROUTE_TENANT_SERVICE_BEAN)
        .getBean(ApiGatewayConfigurationProperties.class)
        .extracting(ApiGatewayConfigurationProperties::getUrl)
        .isEqualTo("http://localhost:8001"));
  }

  @Test
  void autoConfiguration_negative_propertyNotSet() {
    contextRunner.run(context -> assertThat(context)
      .doesNotHaveBean(ADMIN_CLIENT_BEAN)
      .doesNotHaveBean(GATEWAY_SERVICE_BEAN)
      .doesNotHaveBean(ROUTE_TENANT_SERVICE_BEAN));
  }

  @Test
  void autoConfiguration_negative_disabled() {
    contextRunner
      .withPropertyValues("application.apigw.enabled=false", "application.apigw.url=http://localhost:8001")
      .run(context -> assertThat(context)
        .doesNotHaveBean(ADMIN_CLIENT_BEAN)
        .doesNotHaveBean(GATEWAY_SERVICE_BEAN)
        .doesNotHaveBean(ROUTE_TENANT_SERVICE_BEAN));
  }

  @Test
  void autoConfiguration_positive_moduleRegistrationDisabledByDefault() {
    contextRunner
      .withPropertyValues("application.apigw.enabled=true", "application.apigw.url=http://localhost:8001")
      .run(context -> assertThat(context).doesNotHaveBean(MODULE_REGISTRAR_BEAN));
  }

  @Test
  void autoConfiguration_positive_moduleRegistrationEnabled() {
    contextRunner
      .withPropertyValues("application.apigw.enabled=true", "application.apigw.url=http://localhost:8001",
        "application.apigw.register-module=true")
      .run(context -> assertThat(context).hasBean(MODULE_REGISTRAR_BEAN));
  }
}
