package org.folio.tools.apisix.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.json.JsonMapper;

@UnitTest
class ApisixGatewayAutoConfigurationTest {

  private static final String ADMIN_CLIENT_BEAN = "folioApisixAdminClient";
  private static final String GATEWAY_SERVICE_BEAN = "folioApisixGatewayService";
  private static final String ROUTE_TENANT_SERVICE_BEAN = "folioApisixRouteTenantService";
  private static final String MODULE_REGISTRAR_BEAN = "folioApiGatewayModuleRegistrar";

  private static final String[] APISIX_PROPERTIES = {
    "application.apigw.enabled=true",
    "application.apigw.type=apisix",
    "application.apigw.url=http://localhost:9180",
    "application.apigw.api-key=test-admin-key"};

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(ApisixGatewayAutoConfiguration.class))
    .withBean(JsonMapper.class, JsonMapper::new);

  @Test
  void autoConfiguration_positive_typeApisix() {
    contextRunner
      .withPropertyValues(APISIX_PROPERTIES)
      .run(context -> assertThat(context)
        .hasBean(ADMIN_CLIENT_BEAN)
        .hasBean(GATEWAY_SERVICE_BEAN)
        .hasBean(ROUTE_TENANT_SERVICE_BEAN)
        .getBean(ApisixGatewayConfigurationProperties.class)
        .satisfies(properties -> {
          assertThat(properties.getUrl()).isEqualTo("http://localhost:9180");
          assertThat(properties.getApiKey()).isEqualTo("test-admin-key");
        }));
  }

  @Test
  void autoConfiguration_negative_typeKong() {
    contextRunner
      .withPropertyValues("application.apigw.enabled=true", "application.apigw.type=kong",
        "application.apigw.url=http://localhost:8001")
      .run(context -> assertThat(context)
        .doesNotHaveBean(ADMIN_CLIENT_BEAN)
        .doesNotHaveBean(GATEWAY_SERVICE_BEAN)
        .doesNotHaveBean(ROUTE_TENANT_SERVICE_BEAN));
  }

  @Test
  void autoConfiguration_negative_typeNotSet() {
    contextRunner
      .withPropertyValues("application.apigw.enabled=true", "application.apigw.url=http://localhost:9180")
      .run(context -> assertThat(context)
        .doesNotHaveBean(ADMIN_CLIENT_BEAN)
        .doesNotHaveBean(GATEWAY_SERVICE_BEAN)
        .doesNotHaveBean(ROUTE_TENANT_SERVICE_BEAN));
  }

  @Test
  void autoConfiguration_negative_notEnabled() {
    contextRunner
      .withPropertyValues("application.apigw.type=apisix", "application.apigw.url=http://localhost:9180")
      .run(context -> assertThat(context)
        .doesNotHaveBean(ADMIN_CLIENT_BEAN)
        .doesNotHaveBean(GATEWAY_SERVICE_BEAN)
        .doesNotHaveBean(ROUTE_TENANT_SERVICE_BEAN));
  }

  @Test
  void autoConfiguration_negative_missingApiKey() {
    contextRunner
      .withPropertyValues("application.apigw.enabled=true", "application.apigw.type=apisix",
        "application.apigw.url=http://localhost:9180")
      .run(context -> {
        assertThat(context).hasFailed();
        assertThat(context.getStartupFailure()).rootCause()
          .hasMessageContaining("APIGW_API_KEY");
      });
  }

  @Test
  void autoConfiguration_positive_moduleRegistrationDisabledByDefault() {
    contextRunner
      .withPropertyValues(APISIX_PROPERTIES)
      .run(context -> assertThat(context).doesNotHaveBean(MODULE_REGISTRAR_BEAN));
  }

  @Test
  void autoConfiguration_positive_moduleRegistrationEnabled() {
    contextRunner
      .withPropertyValues(APISIX_PROPERTIES)
      .withPropertyValues("application.apigw.register-module=true")
      .run(context -> assertThat(context).hasBean(MODULE_REGISTRAR_BEAN));
  }
}
