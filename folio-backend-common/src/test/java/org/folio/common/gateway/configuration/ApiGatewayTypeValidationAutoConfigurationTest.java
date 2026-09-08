package org.folio.common.gateway.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@UnitTest
class ApiGatewayTypeValidationAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(ApiGatewayTypeValidationAutoConfiguration.class));

  @ParameterizedTest
  @ValueSource(strings = {"kong", "apisix", "KONG", "Apisix"})
  void autoConfiguration_positive_supportedType(String type) {
    contextRunner
      .withPropertyValues("application.apigw.enabled=true", "application.apigw.type=" + type)
      .run(context -> assertThat(context)
        .hasNotFailed()
        .hasSingleBean(ApiGatewayTypeValidationAutoConfiguration.class));
  }

  @Test
  void autoConfiguration_positive_typeNotSet() {
    contextRunner
      .withPropertyValues("application.apigw.enabled=true")
      .run(context -> assertThat(context)
        .hasNotFailed()
        .hasSingleBean(ApiGatewayTypeValidationAutoConfiguration.class));
  }

  @ParameterizedTest
  @ValueSource(strings = {"apisx", "", "nginx"})
  void autoConfiguration_negative_unsupportedType(String type) {
    contextRunner
      .withPropertyValues("application.apigw.enabled=true", "application.apigw.type=" + type)
      .run(context -> {
        assertThat(context).hasFailed();
        assertThat(context.getStartupFailure()).rootCause()
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Unsupported API Gateway type: '" + type + "'")
          .hasMessageContaining("Supported values: kong, apisix")
          .hasMessageContaining("APIGW_TYPE");
      });
  }

  @Test
  void autoConfiguration_negative_gatewayDisabled() {
    contextRunner
      .withPropertyValues("application.apigw.enabled=false", "application.apigw.type=apisx")
      .run(context -> assertThat(context)
        .hasNotFailed()
        .doesNotHaveBean(ApiGatewayTypeValidationAutoConfiguration.class));
  }

  @Test
  void autoConfiguration_negative_gatewayPropertyNotSet() {
    contextRunner
      .withPropertyValues("application.apigw.type=apisx")
      .run(context -> assertThat(context)
        .hasNotFailed()
        .doesNotHaveBean(ApiGatewayTypeValidationAutoConfiguration.class));
  }
}
