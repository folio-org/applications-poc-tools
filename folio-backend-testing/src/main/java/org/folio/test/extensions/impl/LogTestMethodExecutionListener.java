package org.folio.test.extensions.impl;

import lombok.extern.log4j.Log4j2;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

@Log4j2(topic = "TestMethod")
public class LogTestMethodExecutionListener implements TestExecutionListener {

  @Override
  public void beforeTestExecution(TestContext testContext) {
    log.info("[Running]: {}", testContext.getTestMethod().getName());
  }

  @Override
  public void afterTestExecution(TestContext testContext) {
    log.info("[Finished]: {}", testContext.getTestMethod().getName());
  }
}
