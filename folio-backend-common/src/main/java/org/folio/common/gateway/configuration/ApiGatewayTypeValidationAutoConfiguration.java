package org.folio.common.gateway.configuration;

import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Fails startup early when {@code application.apigw.type} selects a gateway that no integration library implements.
 *
 * <p>Each gateway auto-configuration activates only for its own {@code application.apigw.type} value, so an
 * unsupported value would otherwise activate no gateway at all, and a module that relies on gateway
 * self-registration would start without registering itself anywhere.</p>
 */
@Configuration
@ConditionalOnProperty("application.apigw.enabled")
public class ApiGatewayTypeValidationAutoConfiguration {

  static final String GATEWAY_TYPE_PROPERTY = "application.apigw.type";
  static final String DEFAULT_GATEWAY_TYPE = "kong";
  private static final Set<String> SUPPORTED_GATEWAY_TYPES = Set.of("kong", "apisix");

  /**
   * Validates the configured API Gateway type while the application context is being built.
   *
   * @param environment - Spring {@link Environment} providing {@code application.apigw.type}
   */
  public ApiGatewayTypeValidationAutoConfiguration(Environment environment) {
    validateGatewayType(environment.getProperty(GATEWAY_TYPE_PROPERTY, DEFAULT_GATEWAY_TYPE));
  }

  // Case-insensitive on purpose: @ConditionalOnProperty(havingValue = ...) matches with equalsIgnoreCase,
  // so the check must accept exactly the values that activate a gateway auto-configuration.
  static void validateGatewayType(String type) {
    if (SUPPORTED_GATEWAY_TYPES.stream().noneMatch(type::equalsIgnoreCase)) {
      throw new IllegalStateException("Unsupported API Gateway type: '" + type
        + "'. Supported values: kong, apisix (application.apigw.type / APIGW_TYPE)");
    }
  }
}
