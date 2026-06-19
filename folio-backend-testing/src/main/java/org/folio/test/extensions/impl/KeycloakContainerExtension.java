package org.folio.test.extensions.impl;

import static dasniko.testcontainers.keycloak.ExtendableKeycloakContainer.ADMIN_CLI_CLIENT;
import static dasniko.testcontainers.keycloak.ExtendableKeycloakContainer.MASTER_REALM;
import static jakarta.ws.rs.client.ClientBuilder.newBuilder;
import static java.lang.String.format;
import static org.apache.http.conn.ssl.NoopHostnameVerifier.INSTANCE;
import static org.apache.http.ssl.SSLContextBuilder.create;
import static org.awaitility.Awaitility.await;
import static org.folio.test.TestUtils.parse;
import static org.folio.test.TestUtils.readString;
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
import org.keycloak.representations.idm.PartialImportRepresentation;

@Log4j2
public class KeycloakContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String RUN_MODE = "dev";
  private static final String LOG_LEVEL = "INFO";
  private static final String FOLIO_BACKEND_ADMIN_CLIENT = "folio-backend-admin-client";
  private static final String FOLIO_BACKEND_ADMIN_CLIENT_SECRET = "supersecret";

  private static final String SSL_KEYSTORE_PATH = "certificates/test.keystore.jks";
  private static final String SSL_TRUSTSTORE_PATH = "classpath:certificates/test.truststore.jks";
  private static final String SSL_KEYSTORE_PASSWORD = "secretpassword";
  private static final String SSL_KEYSTORE_TYPE = "JKS";

  private static final String REALM_JSON = "json/keycloak/master-realm.json";

  private static final KeycloakContainer CONTAINER = keycloakContainer(getKeycloakImageName());

  private static Keycloak ADMIN_CLIENT;

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();

      await().atMost(60, TimeUnit.SECONDS).until(
        () -> CONTAINER.isRunning() && containerIsReady()
      );
    }

    ADMIN_CLIENT = keycloakAdminClient();

    setupMasterRealm();

    System.setProperty("KC_URL", CONTAINER.getAuthServerUrl());
    System.setProperty("KC_ADMIN_CLIENT_ID", FOLIO_BACKEND_ADMIN_CLIENT);
    System.setProperty("KC_ADMIN_CLIENT_SECRET", FOLIO_BACKEND_ADMIN_CLIENT_SECRET);
    System.setProperty("KC_ADMIN_USERNAME", CONTAINER.getAdminUsername());
    System.setProperty("KC_ADMIN_PASSWORD", CONTAINER.getAdminPassword());
    System.setProperty("KC_ADMIN_GRANT_TYPE", OAuth2Constants.CLIENT_CREDENTIALS);
  }

  private static boolean containerIsReady() {
    return CONTAINER.getLogs()
      .contains(format("Admin client '%s' has been created successfully", FOLIO_BACKEND_ADMIN_CLIENT));
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
    if (ADMIN_CLIENT == null) {
      throw new IllegalStateException("Keycloak admin client isn't initialized");
    }

    return ADMIN_CLIENT;
  }

  private static void setupMasterRealm() {
    log.info("Setting up master realm");
    var realmJson = readString(REALM_JSON);
    var realmPartialImport = parse(realmJson, PartialImportRepresentation.class);
    var masterRealm = ADMIN_CLIENT.realm(MASTER_REALM);
    try (var response = masterRealm.partialImport(realmPartialImport)) {
      if (response.getStatus() >= 400) {
        log.warn("Failed to partially import master realm: reason = {}", response.getEntity());
      }
    }

    var masterRealmRepresentation = ADMIN_CLIENT.realm(MASTER_REALM).toRepresentation();
    masterRealmRepresentation.setAccessTokenLifespan(900);
    ADMIN_CLIENT.realm(MASTER_REALM).update(masterRealmRepresentation);
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
      .withEnv("KC_LOG_LEVEL", LOG_LEVEL)
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
