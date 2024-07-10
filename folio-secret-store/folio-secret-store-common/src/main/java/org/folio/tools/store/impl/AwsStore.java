package org.folio.tools.store.impl;

import static java.lang.Boolean.TRUE;
import static software.amazon.awssdk.services.ssm.model.ParameterType.SECURE_STRING;

import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.NotFoundException;
import org.folio.tools.store.properties.AwsConfigProperties;
import org.folio.tools.store.utils.SsmClientProvider;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

@Log4j2
@RequiredArgsConstructor
public final class AwsStore implements SecureStore {

  public static final String TYPE = "AwsSsm";
  public static final String PROP_REGION = "region";

  private final SsmClientProvider ssmClientProvider;

  @Override
  public String get(String clientId, String tenant, String username) {
    try (var ssmClient = ssmClientProvider.get()) {
      var key = String.format("%s_%s_%s", clientId, tenant, username);
      var request = GetParameterRequest.builder().name(key).withDecryption(true).build();
      return getParameter(ssmClient, request);
    }
  }

  @Override
  public String get(String key) {
    try (var ssmClient = ssmClientProvider.get()) {
      var request = GetParameterRequest.builder().name(key).withDecryption(true).build();
      return getParameter(ssmClient, request);
    }
  }

  @Override
  public void set(String key, String value) {
    try (var ssmClient = ssmClientProvider.get()) {
      var request = PutParameterRequest.builder().name(key).value(value).overwrite(TRUE).type(SECURE_STRING).build();
      ssmClient.putParameter(request);
    }
  }

  /**
   * Creates {@link AwsStore} component.
   *
   * @param properties - configuration properties
   * @return created {@link AwsStore} component
   */
  public static AwsStore create(Properties properties) {
    return new AwsStore(new SsmClientProvider(properties));
  }

  /**
   * Creates {@link AwsStore} component.
   *
   * @param properties - configuration properties
   * @return created {@link AwsStore} component
   */
  public static AwsStore create(AwsConfigProperties properties) {
    return new AwsStore(new SsmClientProvider(properties));
  }

  private String getParameter(SsmClient ssmClient, GetParameterRequest request) {
    try {
      return ssmClient.getParameter(request).parameter().value();
    } catch (Exception e) {
      throw new NotFoundException(e);
    }
  }
}
