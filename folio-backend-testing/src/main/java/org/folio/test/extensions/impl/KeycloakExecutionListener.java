package org.folio.test.extensions.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.folio.test.TestUtils.parse;
import static org.folio.test.extensions.impl.KeycloakContainerExtension.getKeycloakAdminClient;
import static org.springframework.core.annotation.AnnotatedElementUtils.getMergedRepeatableAnnotations;
import static org.springframework.test.context.util.TestContextResourceUtils.convertToClasspathResourcePaths;
import static org.springframework.test.context.util.TestContextResourceUtils.convertToResourceList;

import jakarta.ws.rs.NotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.folio.test.extensions.KeycloakRealms;
import org.folio.test.extensions.KeycloakRealmsGroup;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

@Log4j2
public class KeycloakExecutionListener implements TestExecutionListener {

  @Override
  public void beforeTestMethod(@NonNull TestContext ctx) {
    var adminClient = getKeycloakAdminClient();
    var annotatedRealms = getAnnotatedRealms(ctx);
    var createdRealms = new ArrayList<String>();
    for (var realmRepresentation : annotatedRealms) {
      log.info("Importing test realm: {}", realmRepresentation.getRealm());
      createdRealms.add(realmRepresentation.getRealm());
      adminClient.realms().create(realmRepresentation);
    }

    ctx.setAttribute(getImportedRealmsAttributeName(), createdRealms);
  }

  @Override
  public void afterTestMethod(@NonNull TestContext ctx) {
    var keycloak = getKeycloakAdminClient();
    var importedRawRealmNames = ctx.getAttribute(getImportedRealmsAttributeName());

    //noinspection rawtypes
    if (importedRawRealmNames instanceof List importedRealmNamesList) {
      for (var rawRealmName : importedRealmNamesList) {
        if (rawRealmName instanceof String realmName) {
          try {
            keycloak.realm(realmName).remove();
          } catch (NotFoundException e) {
            // nothing to do, cause realm is not found
          }
        }
      }
    }
  }

  private static Set<KeycloakRealms> getAnnotationsFor(AnnotatedElement element) {
    return getMergedRepeatableAnnotations(element, KeycloakRealms.class, KeycloakRealmsGroup.class);
  }

  private static List<RealmRepresentation> getAnnotatedRealms(TestContext testContext) {
    var realmsMap = new ArrayList<RealmRepresentation>();
    for (var keycloakRealms : getAnnotationsFor(testContext.getTestClass())) {
      realmsMap.addAll(getRealmRepresentations(keycloakRealms, testContext));
    }

    for (var keycloakRealms : getAnnotationsFor(testContext.getTestMethod())) {
      realmsMap.addAll(getRealmRepresentations(keycloakRealms, testContext));
    }

    return realmsMap;
  }

  private static List<RealmRepresentation> getRealmRepresentations(KeycloakRealms keycloakRealms, TestContext ctx) {
    var realmPaths = keycloakRealms.realms();
    var classpathResourcePaths = convertToClasspathResourcePaths(ctx.getTestClass(), realmPaths);
    var resources = convertToResourceList(ctx.getApplicationContext(), classpathResourcePaths);
    var realms = new ArrayList<RealmRepresentation>();
    for (var resource : resources) {
      var realmJson = readAsString(resource);
      var realmRepresentation = parse(realmJson, RealmRepresentation.class);
      realms.add(realmRepresentation);
    }

    return realms;
  }

  private static String readAsString(Resource resource) {
    try {
      return IOUtils.toString(resource.getInputStream(), UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read resource as string: " + resource.getFilename(), e);
    }
  }

  private String getImportedRealmsAttributeName() {
    return this.getClass() + "#kc-imported-realms";
  }
}
