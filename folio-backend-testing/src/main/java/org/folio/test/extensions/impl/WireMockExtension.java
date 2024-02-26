package org.folio.test.extensions.impl;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.makeAccessible;
import static org.springframework.util.ReflectionUtils.setField;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class WireMockExtension implements BeforeAllCallback, AfterAllCallback {

  public static final int WM_DOCKER_PORT = 8080;
  public static final String WM_NETWORK_ALIAS = UUID.randomUUID().toString();
  public static final String WM_URL_PROPERTY = "wm.url";

  private static final DockerImageName WM_IMAGE = DockerImageName.parse("wiremock/wiremock:2.35.0");
  private static final String WM_URL_VARS_FILE = "wiremock-url.vars";

  @SuppressWarnings("resource")
  private static final GenericContainer<?> WM_CONTAINER = new GenericContainer<>(WM_IMAGE)
    .withNetwork(Network.SHARED)
    .withExposedPorts(WM_DOCKER_PORT)
    .withAccessToHost(true)
    .withNetworkAliases(WM_NETWORK_ALIAS)
    .withCommand(
      "--local-response-templating",
      "--disable-banner",
      "--verbose"
    )
    .withLogConsumer(
      new Slf4jLogConsumer(log).withSeparateOutputStreams()
    );

  private static WireMockAdminClient ADMIN_CLIENT;

  public static WireMockAdminClient getWireMockAdminClient() {
    if (ADMIN_CLIENT == null) {
      throw new IllegalStateException("WireMock admin client isn't initialized");
    }

    return ADMIN_CLIENT;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    runContainer();

    String wmUrl = getUrlForExposedPort();
    setProperty(WM_URL_PROPERTY, wmUrl);

    setSystemVarsToWireMockUrl(context, wmUrl);

    assignAdminClientFieldInTestClass(context.getRequiredTestClass());
  }

  @Override
  public void afterAll(ExtensionContext context) {
    clearProperty(WM_URL_PROPERTY);

    clearSystemVarsWithWireMockUrl(context);
  }

  @SneakyThrows
  private static void runContainer() {
    if (!WM_CONTAINER.isRunning()) {
      WM_CONTAINER.start();

      var wmUrl = getUrlForExposedPort();
      log.info("Wire mock server started [url: {}]", wmUrl);

      int hostPort = WM_CONTAINER.getMappedPort(WM_DOCKER_PORT);
      Testcontainers.exposeHostPorts(hostPort);
      log.info("Host port exposed to containers: {}", hostPort);

      ADMIN_CLIENT = new WireMockAdminClient(wmUrl);
    }
  }

  private void setSystemVarsToWireMockUrl(ExtensionContext context, String wmUrl) {
    List<String> vars = readWireMockUrlVars(context);

    log.debug("Assigning WireMock url to system variables: {}", vars);
    vars.forEach(env -> setProperty(env, wmUrl));
  }

  private void clearSystemVarsWithWireMockUrl(ExtensionContext context) {
    List<String> vars = readWireMockUrlVars(context);

    log.debug("Clearing system variables with WireMock url: {}", vars);
    vars.forEach(System::clearProperty);
  }

  @SneakyThrows
  private static List<String> readWireMockUrlVars(ExtensionContext context) {
    var cl = context.getRequiredTestClass().getClassLoader();

    var url = cl.getResource(WM_URL_VARS_FILE);
    if (url == null) {
      return Collections.emptyList();
    }

    return FileUtils.readLines(new File(url.toURI()), StandardCharsets.UTF_8);
  }

  private void assignAdminClientFieldInTestClass(Class<?> testClass) {
    var adminClientField = findField(testClass, null, WireMockAdminClient.class);
    if (adminClientField != null) {
      makeAccessible(adminClientField);

      setField(adminClientField, testClass, getWireMockAdminClient());
      log.debug("WireMock admin client set to test class' field: field = {}, testClass = {}",
        adminClientField.getName(), testClass.getSimpleName());
    }
  }

  private static String getUrlForExposedPort() {
    return String.format("http://%s:%s", WM_CONTAINER.getHost(), WM_CONTAINER.getMappedPort(WM_DOCKER_PORT));
  }
}
