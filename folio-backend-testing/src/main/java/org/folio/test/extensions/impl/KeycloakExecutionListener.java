package org.folio.test.extensions.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.folio.test.TestUtils.parse;
import static org.folio.test.extensions.impl.KeycloakContainerExtension.getKeycloakAdminClient;
import static org.springframework.core.annotation.AnnotatedElementUtils.getMergedRepeatableAnnotations;
import static org.springframework.test.context.util.TestContextResourceUtils.convertToClasspathResourcePaths;
import static org.springframework.test.context.util.TestContextResourceUtils.convertToResourceList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.extensions.KeycloakRealmsGroup;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.Assert;

@Log4j2
public class KeycloakExecutionListener implements TestExecutionListener {

  @Override
  public void beforeTestMethod(@NonNull TestContext ctx) {
    var adminClient = getKeycloakAdminClient();
    getAnnotationsFor(ctx.getTestClass()).forEach(realmPath -> importKeycloakRealm(realmPath, adminClient, ctx));
    getAnnotationsFor(ctx.getTestMethod()).forEach(realmPath -> importKeycloakRealm(realmPath, adminClient, ctx));
  }

  @Override
  public void afterTestMethod(@NonNull TestContext ctx) {
    var keycloak = getKeycloakAdminClient();
    var realms = keycloak.realms().findAll();
    realms.stream()
      .map(RealmRepresentation::getRealm)
      .filter(realm -> !realm.equals("master"))
      .forEach(realm -> {
        log.info("Removing test realm: {}", realm);
        keycloak.realms().realm(realm).remove();
      });
  }

  private static Set<KeycloakRealms> getAnnotationsFor(AnnotatedElement element) {
    return getMergedRepeatableAnnotations(element, KeycloakRealms.class, KeycloakRealmsGroup.class);
  }

  private static void importKeycloakRealm(KeycloakRealms keycloakRealms, Keycloak keycloak, TestContext ctx) {
    var realms = keycloakRealms.realms();
    Assert.notEmpty(realms, "Empty realms are not allowed");

    var classpathResourcePaths = convertToClasspathResourcePaths(ctx.getTestClass(), realms);
    var resources = convertToResourceList(ctx.getApplicationContext(), classpathResourcePaths);
    for (var resource : resources) {
      var realmJson = readAsString(resource);
      var realmRepresentation = parse(realmJson, RealmRepresentation.class);
      log.info("Importing test realm: {}", realmRepresentation.getRealm());
      keycloak.realms().create(realmRepresentation);
    }
  }

  private static String readAsString(Resource resource) {
    try {
      return IOUtils.toString(resource.getInputStream(), UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read resource as string: " + resource.getFilename(), e);
    }
  }
}
