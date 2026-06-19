package org.folio.test.extensions.impl;

import static dasniko.testcontainers.keycloak.ExtendableKeycloakContainer.ADMIN_CLI_CLIENT;
import static dasniko.testcontainers.keycloak.ExtendableKeycloakContainer.MASTER_REALM;
import static jakarta.ws.rs.client.ClientBuilder.newBuilder;
import static java.lang.String.format;
import static org.apache.http.conn.ssl.NoopHostnameVerifier.INSTANCE;
import static org.apache.http.ssl.SSLContextBuilder.create;
import static org.awaitility.Awaitility.await;
import static org.folio.test.extensions.impl.DockerImageRegistry.getKeycloakImageName;
import static org.springframework.util.ResourceUtils.getFile;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.ssl.SSLInitializationException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.JacksonProvider;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

@Log4j2
public class KeycloakContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String ENV_KEYCLOAK_LOG_LEVEL = "TESTCONTAINERS_KEYCLOAK_LOG_LEVEL";
  private static final String ENV_KEYCLOAK_READINESS_TIMEOUT = "TESTCONTAINERS_KEYCLOAK_READINESS_TIMEOUT";

  private static final String DEFAULT_LOG_LEVEL = "INFO";
  private static final long DEFAULT_CONTAINER_READINESS_TIMEOUT = 60; // in seconds

  private static final String RUN_MODE = "dev";
  private static final String FOLIO_BACKEND_ADMIN_CLIENT = "folio-backend-admin-client";
  private static final String FOLIO_BACKEND_ADMIN_CLIENT_SECRET = "supersecret";

  private static final String SSL_KEYSTORE_PATH = "certificates/test.keystore.jks";
  private static final String SSL_TRUSTSTORE_PATH = "classpath:certificates/test.truststore.jks";
  private static final String SSL_KEYSTORE_PASSWORD = "secretpassword";
  private static final String SSL_KEYSTORE_TYPE = "JKS";

  private static final String CONTAINER_LOG_LEVEL;
  private static final long CONTAINER_READINESS_TIMEOUT;
  private static final KeycloakContainer CONTAINER;

  private static Keycloak adminClient;

  static {
    var env = System.getenv();

    CONTAINER_LOG_LEVEL = env.getOrDefault(ENV_KEYCLOAK_LOG_LEVEL, DEFAULT_LOG_LEVEL);
    CONTAINER_READINESS_TIMEOUT = Long.parseLong(
      env.getOrDefault(ENV_KEYCLOAK_READINESS_TIMEOUT, String.valueOf(DEFAULT_CONTAINER_READINESS_TIMEOUT)));

    CONTAINER = keycloakContainer(getKeycloakImageName());
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();

      // CONTAINER.start() returns as soon as Keycloak's HTTP endpoint responds, but the folio-keycloak
      // image runs configure-realms.sh in the background (see folio/start.sh in folio-org/folio-keycloak).
      // That script creates the folio-backend-admin-client via setup-admin-client.sh, which must complete
      // before any test can authenticate. The extra await below blocks until setup-admin-client.sh logs
      // its success message, confirming all custom initialization is done.
      await().atMost(CONTAINER_READINESS_TIMEOUT, TimeUnit.SECONDS).until(
        () -> CONTAINER.isRunning() && containerIsReady()
      );
    }

    adminClient = keycloakAdminClient();

    setupMasterRealm();

    System.setProperty("KC_URL", CONTAINER.getAuthServerUrl());
    System.setProperty("KC_ADMIN_CLIENT_ID", FOLIO_BACKEND_ADMIN_CLIENT);
    System.setProperty("KC_ADMIN_CLIENT_SECRET", FOLIO_BACKEND_ADMIN_CLIENT_SECRET);
    System.setProperty("KC_ADMIN_USERNAME", CONTAINER.getAdminUsername());
    System.setProperty("KC_ADMIN_PASSWORD", CONTAINER.getAdminPassword());
    System.setProperty("KC_ADMIN_GRANT_TYPE", OAuth2Constants.CLIENT_CREDENTIALS);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    System.clearProperty("KC_URL");
    System.clearProperty("KC_ADMIN_CLIENT_ID");
    System.clearProperty("KC_ADMIN_CLIENT_SECRET");
    System.clearProperty("KC_ADMIN_USERNAME");
    System.clearProperty("KC_ADMIN_PASSWORD");
    System.clearProperty("KC_ADMIN_GRANT_TYPE");
  }

  public static Keycloak getKeycloakAdminClient() {
    if (adminClient == null) {
      throw new IllegalStateException("Keycloak admin client isn't initialized");
    }

    return adminClient;
  }

  // Checks for the success log line emitted by folio/setup-admin-client.sh in folio-org/folio-keycloak
  // once the admin client has been fully created. Log-scanning is the only available signal because
  // configure-realms.sh is launched as a background process and has no hook into the Keycloak wait strategy.
  // NOTE: if the log message in setup-admin-client.sh is ever changed, this check will silently
  // time out — update the expected string here accordingly.
  private static boolean containerIsReady() {
    return CONTAINER.getLogs()
      .contains(format("Admin client '%s' has been created successfully", FOLIO_BACKEND_ADMIN_CLIENT));
  }

  private static void setupMasterRealm() {
    log.info("Setting up master realm");

    var masterRealmRepresentation = adminClient.realm(MASTER_REALM).toRepresentation();
    masterRealmRepresentation.setAccessTokenLifespan(900);
    adminClient.realm(MASTER_REALM).update(masterRealmRepresentation);
  }

  private static Keycloak keycloakAdminClient() {
    return KeycloakBuilder.builder()
      .realm(MASTER_REALM)
      .serverUrl(CONTAINER.getAuthServerUrl())
      .clientId(ADMIN_CLI_CLIENT)
      .username(CONTAINER.getAdminUsername())
      .password(CONTAINER.getAdminPassword())
      .grantType("password")
      .resteasyClient(buildResteasyClient())
      .build();
  }

  @SuppressWarnings("resource")
  private static KeycloakContainer keycloakContainer(String keycloakImageName) {
    return new KeycloakContainer(keycloakImageName)
      .withEnv("KC_RUN_MODE", RUN_MODE)
      .withAdminUsername("keycloak-test-admin")
      .withAdminPassword(RandomStringUtils.secure().next(20, true, true))
      .withEnv("KC_FOLIO_BE_ADMIN_CLIENT_ID", FOLIO_BACKEND_ADMIN_CLIENT)
      .withEnv("KC_FOLIO_BE_ADMIN_CLIENT_SECRET", FOLIO_BACKEND_ADMIN_CLIENT_SECRET)
      .withEnv("KC_HTTPS_KEY_STORE_TYPE", SSL_KEYSTORE_TYPE)
      .withEnv("KC_LOG_LEVEL", CONTAINER_LOG_LEVEL)
      .withFeaturesEnabled("scripts:v1", "token-exchange:v1", "admin-fine-grained-authz:v1")
      //.withVerboseOutput()
      .useTlsKeystore(SSL_KEYSTORE_PATH, SSL_KEYSTORE_PASSWORD);
  }

  private static ResteasyClient buildResteasyClient() {
    return (ResteasyClient) newBuilder()
      .sslContext(getSslContext())
      .hostnameVerifier(INSTANCE)
      .register(JacksonProvider.class)
      .build();
  }

  private static SSLContext getSslContext() {
    try {
      return create()
        .loadTrustMaterial(getFile(SSL_TRUSTSTORE_PATH), SSL_KEYSTORE_PASSWORD.toCharArray())
        .build();
    } catch (Exception e) {
      log.error("Error creating SSL context", e);
      throw new SSLInitializationException("Error creating SSL context", e);
    }
  }
}
