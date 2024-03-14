package org.folio.test.extensions.impl;

import static dasniko.testcontainers.keycloak.ExtendableKeycloakContainer.ADMIN_CLI_CLIENT;
import static dasniko.testcontainers.keycloak.ExtendableKeycloakContainer.MASTER_REALM;
import static jakarta.ws.rs.client.ClientBuilder.newBuilder;
import static org.apache.http.conn.ssl.NoopHostnameVerifier.INSTANCE;
import static org.apache.http.ssl.SSLContextBuilder.create;
import static org.folio.test.TestUtils.parse;
import static org.folio.test.TestUtils.readString;
import static org.folio.test.TestUtils.readToFile;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.util.ResourceUtils.getFile;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.List;
import javax.net.ssl.SSLContext;
import lombok.extern.log4j.Log4j2;
import org.apache.http.ssl.SSLInitializationException;
import org.folio.test.extensions.EnableKeycloak;
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

  private static final String REALM_JSON = "json/keycloak/master-realm.json";
  private static final String IMPORTED_CLIENT_ID = "folio-backend-admin-client";
  private static final String IMPORTED_CLIENT_SECRET = "supersecret";
  private static Keycloak ADMIN_CLIENT;

  @Override
  public void beforeAll(ExtensionContext context) {
    var container = getKeycloakContainer(context);
    if (!container.isRunning()) {
      container.start();
    }

    ADMIN_CLIENT = keycloakAdminClient(context, container);

    setupMasterRealm();

    System.setProperty("KC_URL", container.getAuthServerUrl());
    System.setProperty("KC_ADMIN_CLIENT_ID", IMPORTED_CLIENT_ID);
    System.setProperty("KC_ADMIN_CLIENT_SECRET", IMPORTED_CLIENT_SECRET);
    System.setProperty("KC_ADMIN_USERNAME", container.getAdminUsername());
    System.setProperty("KC_ADMIN_PASSWORD", container.getAdminPassword());
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

  @SuppressWarnings("resource")
  private static KeycloakContainer getKeycloakContainer(ExtensionContext context) {
    return isUseTls(context)
      ? keycloakContainer().useTlsKeystore("certificates/test.keystore.jks", "secretpassword")
      : keycloakContainer();
  }

  private static boolean isUseTls(ExtensionContext context) {
    var targetClass = context.getRequiredTestClass();
    var annotation = findAnnotation(targetClass, EnableKeycloak.class);
    return annotation.tlsEnabled();
  }

  private static Keycloak keycloakAdminClient(ExtensionContext context, KeycloakContainer container) {
    var builder = KeycloakBuilder.builder()
      .realm(MASTER_REALM)
      .serverUrl(container.getAuthServerUrl())
      .clientId(ADMIN_CLI_CLIENT)
      .username(container.getAdminUsername())
      .password(container.getAdminPassword())
      .grantType("password");

    if (isUseTls(context)) {
      builder.resteasyClient(buildResteasyClient());
    }

    return builder.build();
  }

  @SuppressWarnings("resource")
  private static KeycloakContainer keycloakContainer() {
    return new KeycloakContainer("quay.io/keycloak/keycloak:24.0.0")
      .withFeaturesEnabled("scripts", "token-exchange", "admin-fine-grained-authz")
      .withProviderLibsFrom(List.of(readToFile("keycloak/folio-scripts.jar", "folio-scripts", ".jar")));
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
