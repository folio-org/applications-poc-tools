package org.folio.test.extensions.impl;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.folio.test.extensions.impl.WireMockExtension.getWireMockAdminClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.core.annotation.AnnotatedElementUtils.getMergedRepeatableAnnotations;
import static org.springframework.test.context.util.TestContextResourceUtils.convertToClasspathResourcePaths;
import static org.springframework.test.context.util.TestContextResourceUtils.convertToResourceList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedElement;
import java.util.Set;
import org.folio.test.extensions.WireMockStub;
import org.folio.test.extensions.WireMockStubGroup;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

public class WireMockExecutionListener implements TestExecutionListener {

  @Override
  public void afterTestExecution(TestContext testContext) {
    if (testContext.getTestException() == null) {
      var unmatched = getWireMockAdminClient().unmatchedRequests().getRequests();

      assertEquals(0, unmatched == null ? 0 : unmatched.size(),
        () -> format("There are unmatched requests to WireMock: %s. Check the mocks are correctly configured",
          unmatched));
    }
  }

  @Override
  public void beforeTestMethod(@NonNull TestContext ctx) {
    var adminClient = getWireMockAdminClient();
    getAnnotationsFor(ctx.getTestClass()).forEach(stub -> populateWiremockStubs(stub, adminClient, ctx));
    getAnnotationsFor(ctx.getTestMethod()).forEach(stub -> populateWiremockStubs(stub, adminClient, ctx));
  }

  @Override
  public void afterTestMethod(@NonNull TestContext ctx) {
    getWireMockAdminClient().resetAll();
  }

  private static Set<WireMockStub> getAnnotationsFor(AnnotatedElement element) {
    return getMergedRepeatableAnnotations(element, WireMockStub.class, WireMockStubGroup.class);
  }

  private static void populateWiremockStubs(WireMockStub stub, WireMockAdminClient client, TestContext ctx) {
    var scripts = stub.scripts();
    Assert.notEmpty(scripts, "Empty scripts are not allowed");

    var classpathResourcePaths = convertToClasspathResourcePaths(ctx.getTestClass(), scripts);
    var resources = convertToResourceList(ctx.getApplicationContext(), classpathResourcePaths);
    for (var resource : resources) {
      client.addStubMapping(readAsString(resource));
    }
  }

  public static String readAsString(Resource resource) {
    try (var reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
      return FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read resource as string: " + resource.getFilename(), e);
    }
  }
}
