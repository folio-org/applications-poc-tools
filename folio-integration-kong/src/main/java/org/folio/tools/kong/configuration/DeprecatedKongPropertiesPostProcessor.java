package org.folio.tools.kong.configuration;

import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * Warns about deprecated Kong-specific API Gateway configuration that has been superseded by the
 * gateway-agnostic {@code application.apigw.*} properties and {@code APIGW_*} environment variables.
 *
 * <p>Detection only: the environment is never modified, so legacy names keep working through the
 * value chains declared by the consuming service.</p>
 *
 * <p>Only the fixed set of legacy FOLIO variables below and the relaxed {@code APPLICATION_KONG_*}
 * form are reported. Kong-the-product container variables such as {@code KONG_PG_*} are left alone.</p>
 *
 * <p>A conflict is reported only when the legacy and the new name are set in the same form - env var
 * against env var, property against property. Cross-form combinations, such as a legacy environment
 * variable together with a new property, degrade to the plain deprecation message.</p>
 */
public class DeprecatedKongPropertiesPostProcessor implements EnvironmentPostProcessor, Ordered {

  private static final int ORDER_OFFSET = 10;
  private static final String LEGACY_PROPERTY_PREFIX = "application.kong.";
  private static final String PROPERTY_PREFIX = "application.apigw.";
  private static final String LEGACY_VARIABLE_PREFIX = "APPLICATION_KONG_";
  private static final String VARIABLE_PREFIX = "APPLICATION_APIGW_";
  private static final String PACKAGED_CONFIG_MARKER = "class path resource [application";

  private static final List<LegacyVariable> LEGACY_VARIABLES = List.of(
    new LegacyVariable("KONG_INTEGRATION_ENABLED", "APIGW_ENABLED", "application.apigw.enabled"),
    new LegacyVariable("KONG_ADMIN_URL", "APIGW_URL", "application.apigw.url"),
    new LegacyVariable("REGISTER_MODULE_IN_KONG", "APIGW_REGISTER_MODULE", "application.apigw.register-module"),
    new LegacyVariable("KONG_RETRIES", "APIGW_RETRIES", "application.apigw.retries"),
    new LegacyVariable("KONG_CONNECT_TIMEOUT", "APIGW_CONNECT_TIMEOUT", "application.apigw.connect-timeout"),
    new LegacyVariable("KONG_READ_TIMEOUT", "APIGW_READ_TIMEOUT", "application.apigw.read-timeout"),
    new LegacyVariable("KONG_WRITE_TIMEOUT", "APIGW_WRITE_TIMEOUT", "application.apigw.write-timeout"),
    new LegacyVariable("KONG_TLS_ENABLED", "APIGW_TLS_ENABLED", "application.apigw.tls.enabled"),
    new LegacyVariable("KONG_TLS_TRUSTSTORE_PATH", "APIGW_TLS_TRUSTSTORE_PATH",
      "application.apigw.tls.trust-store-path"),
    new LegacyVariable("KONG_TLS_TRUSTSTORE_PASSWORD", "APIGW_TLS_TRUSTSTORE_PASSWORD",
      "application.apigw.tls.trust-store-password"),
    new LegacyVariable("KONG_TLS_TRUSTSTORE_TYPE", "APIGW_TLS_TRUSTSTORE_TYPE",
      "application.apigw.tls.trust-store-type"),
    new LegacyVariable("KONG_TENANT_CHECKS_ENABLED", "APIGW_TENANT_CHECKS_ENABLED",
      "application.apigw.tenant-checks.enabled"));

  private final Log log;

  public DeprecatedKongPropertiesPostProcessor(DeferredLogFactory logFactory) {
    this.log = logFactory.getLog(DeprecatedKongPropertiesPostProcessor.class);
  }

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    var variableNames = systemEnvironmentNames(environment);
    warnAboutMappedVariables(variableNames);
    warnAboutRelaxedVariables(variableNames);
    warnAboutProperties(configuredPropertyNames(environment));
  }

  @Override
  public int getOrder() {
    return ConfigDataEnvironmentPostProcessor.ORDER + ORDER_OFFSET;
  }

  private void warnAboutMappedVariables(Set<String> variableNames) {
    for (var variable : LEGACY_VARIABLES) {
      if (!variableNames.contains(variable.name())) {
        continue;
      }
      if (variableNames.contains(variable.replacement()) || variableNames.contains(variable.relaxedReplacement())) {
        log.warn(conflictMessage(variable.replacement(), variable.name()));
      } else {
        log.warn(deprecationMessage("Environment variable", variable.name(), variable.replacement()));
      }
    }
  }

  private void warnAboutRelaxedVariables(Set<String> variableNames) {
    for (var name : variableNames) {
      if (!name.startsWith(LEGACY_VARIABLE_PREFIX)) {
        continue;
      }
      var replacement = VARIABLE_PREFIX + name.substring(LEGACY_VARIABLE_PREFIX.length());
      if (variableNames.contains(replacement)) {
        log.warn(conflictMessage(replacement, name));
      } else {
        log.warn(deprecationMessage("Environment variable", name, replacement));
      }
    }
  }

  private void warnAboutProperties(Set<String> propertyNames) {
    for (var name : propertyNames) {
      if (!name.startsWith(LEGACY_PROPERTY_PREFIX)) {
        continue;
      }
      var replacement = PROPERTY_PREFIX + name.substring(LEGACY_PROPERTY_PREFIX.length());
      if (propertyNames.contains(replacement)) {
        log.warn(conflictMessage(quoted(replacement), quoted(name)));
      } else {
        log.warn(deprecationMessage("Configuration property", quoted(name), quoted(replacement)));
      }
    }
  }

  private static Set<String> systemEnvironmentNames(ConfigurableEnvironment environment) {
    var propertySource = environment.getPropertySources().get(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
    return propertySource instanceof EnumerablePropertySource<?> enumerable
      ? new LinkedHashSet<>(Arrays.asList(enumerable.getPropertyNames()))
      : Set.of();
  }

  private static Set<String> configuredPropertyNames(ConfigurableEnvironment environment) {
    var names = new LinkedHashSet<String>();
    for (var propertySource : environment.getPropertySources()) {
      if (propertySource instanceof EnumerablePropertySource<?> enumerable
        && !propertySource.getName().contains(PACKAGED_CONFIG_MARKER)) {
        names.addAll(Arrays.asList(enumerable.getPropertyNames()));
      }
    }
    return names;
  }

  private static String deprecationMessage(String subject, String name, String replacement) {
    return "%s %s is deprecated and will be removed in the Vetch release. Use %s instead."
      .formatted(subject, name, replacement);
  }

  private static String conflictMessage(String replacement, String name) {
    return "Both %s and deprecated %s are set. %s takes precedence; %s is ignored."
      .formatted(replacement, name, replacement, name);
  }

  private static String quoted(String name) {
    return "'" + name + "'";
  }

  private record LegacyVariable(String name, String replacement, String property) {

    String relaxedReplacement() {
      return property.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
    }
  }
}
