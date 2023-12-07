package org.folio.test.extensions.impl;

import org.folio.test.FakeKafkaConsumer;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

public class KafkaTestExecutionListener implements TestExecutionListener {

  @Override
  public void afterTestMethod(TestContext testContext) {
    FakeKafkaConsumer.removeAllEvents();
  }

  @Override
  public void afterTestClass(TestContext testContext) {
    FakeKafkaConsumer.removeAllEvents();
    FakeKafkaConsumer.stopAllContainers();
  }
}
