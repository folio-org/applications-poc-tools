package org.folio.test.extensions.impl;

import static dasniko.testcontainers.keycloak.ExtendableKeycloakContainer.ADMIN_CLI_CLIENT;
import static dasniko.testcontainers.keycloak.ExtendableKeycloakContainer.MASTER_REALM;
import static jakarta.ws.rs.client.ClientBuilder.newBuilder;
import static org.apache.http.conn.ssl.NoopHostnameVerifier.INSTANCE;
import static org.apache.http.ssl.SSLContextBuilder.create;
import static org.folio.test.TestUtils.parse;
import static org.folio.test.TestUtils.readString;
import static org.folio.test.TestUtils.readToFile;
import static org.springframework.util.ResourceUtils.getFile;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.List;
import javax.net.ssl.SSLContext;
import lombok.extern.log4j.Log4j2;
import org.apache.http.ssl.SSLInitializationException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.PartialImportRepresentation;

@Log4j2
public class KeycloakContainerExtension implements BeforeAllCallback, AfterAllCallback {

  private static final String KEYCLOAK_IMAGE = "quay.io/keycloak/keycloak:24.0.0";
  private static final String REALM_JSON = "json/keycloak/master-realm.json";
  private static final String IMPORTED_CLIENT_ID = "folio-backend-admin-client";
  private static final String IMPORTED_CLIENT_SECRET = "supersecret";
  private static final KeycloakContainer CONTAINER = keycloakContainer();

  private static Keycloak ADMIN_CLIENT;

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!CONTAINER.isRunning()) {
      CONTAINER.start();
    }

    ADMIN_CLIENT = keycloakAdminClient();

    setupMasterRealm();

    System.setProperty("KC_URL", CONTAINER.getAuthServerUrl());
    System.setProperty("KC_ADMIN_CLIENT_ID", IMPORTED_CLIENT_ID);
    System.setProperty("KC_ADMIN_CLIENT_SECRET", IMPORTED_CLIENT_SECRET);
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

    try {
      ADMIN_CLIENT.realm(MASTER_REALM).remove();
      log.info("Master realm removed");
    } finally {
      ADMIN_CLIENT.close();
    }
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
    ADMIN_CLIENT.realm(MASTER_REALM).partialImport(realmPartialImport);
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
  private static KeycloakContainer keycloakContainer() {
    return new KeycloakContainer(KEYCLOAK_IMAGE)
      .withFeaturesEnabled("scripts", "token-exchange", "admin-fine-grained-authz")
      .withProviderLibsFrom(List.of(readToFile("keycloak/folio-scripts.jar", "folio-scripts", ".jar")))
      .useTlsKeystore("certificates/test.keystore.jks", "secretpassword");
  }

  private static ResteasyClient buildResteasyClient() {
    return (ResteasyClient) newBuilder().sslContext(getSslContext()).hostnameVerifier(INSTANCE).build();
  }

  private static SSLContext getSslContext() {
    try {
      return create()
        .loadTrustMaterial(getFile("classpath:certificates/test.truststore.jks"), "secretpassword".toCharArray())
        .build();
    } catch (Exception e) {
      log.error("Error creating SSL context", e);
      throw new SSLInitializationException("Error creating SSL context", e);
    }
  }
}
