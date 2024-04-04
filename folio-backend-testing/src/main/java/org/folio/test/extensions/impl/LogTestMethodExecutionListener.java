package org.folio.test.extensions.impl;

import lombok.extern.log4j.Log4j2;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

@Log4j2(topic = "TestMethod")
public class LogTestMethodExecutionListener implements TestExecutionListener {

  @Override
  public void beforeTestExecution(TestContext testContext) {
    log.info("[{}.{}] running...", className(testContext), testMethodName(testContext));
  }

  @Override
  public void afterTestExecution(TestContext testContext) {
    log.info("[{}.{}] finished", className(testContext), testMethodName(testContext));
  }

  private static String testMethodName(TestContext testContext) {
    return testContext.getTestMethod().getName();
  }

  private static String className(TestContext testContext) {
    return testContext.getTestClass().getSimpleName();
  }
}
