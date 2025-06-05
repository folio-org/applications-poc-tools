package org.folio.tools.store.impl;

import static java.lang.Boolean.TRUE;
import static org.folio.tools.store.impl.Validation.validateKey;
import static org.folio.tools.store.impl.Validation.validateValue;
import static software.amazon.awssdk.services.ssm.model.ParameterType.SECURE_STRING;

import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.tools.store.SecureStore;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.folio.tools.store.exception.SecureStoreServiceException;
import org.folio.tools.store.properties.AwsConfigProperties;
import org.folio.tools.store.utils.SsmClientProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DeleteParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

@Log4j2
@RequiredArgsConstructor
public final class AwsStore implements SecureStore {

  public static final String TYPE = "AwsSsm";
  public static final String PROP_REGION = "region";

  private final SsmClientProvider ssmClientProvider;

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

  @Override
  public String get(String key) {
    validateKey(key);
    try (var ssmClient = ssmClientProvider.get()) {
      var request = GetParameterRequest.builder().name(key).withDecryption(true).build();
      return getParameter(ssmClient, request);
    }
  }

  @Override
  public void set(String key, String value) {
    validateKey(key);
    validateValue(value);
    try (var ssmClient = ssmClientProvider.get()) {
      var request = PutParameterRequest.builder().name(key).value(value).overwrite(TRUE).type(SECURE_STRING).build();
      putParameter(ssmClient, request);
    }
  }

  @Override
  public void delete(String key) {
    validateKey(key);
    try (var ssmClient = ssmClientProvider.get()) {
      var request = DeleteParameterRequest.builder().name(key).build();
      deleteParameter(ssmClient, request);
    }
  }

  private String getParameter(SsmClient ssmClient, GetParameterRequest request) {
    try {
      return ssmClient.getParameter(request).parameter().value();
    } catch (ParameterNotFoundException e) {
      throw new SecretNotFoundException("Parameter not found: " + request.name());
    } catch (SdkException e) {
      throw new SecureStoreServiceException("Failed to get secret: key = " + request.name()
        + ", error = " + e.getMessage(), e);
    }
  }

  private void putParameter(SsmClient ssmClient, PutParameterRequest request) {
    try {
      ssmClient.putParameter(request);
    } catch (SdkException e) {
      throw new SecureStoreServiceException("Failed to save secret: key = " + request.name()
        + ", error = " + e.getMessage(), e);
    }
  }

  private void deleteParameter(SsmClient ssmClient, DeleteParameterRequest request) {
    try {
      ssmClient.deleteParameter(request);
    } catch (ParameterNotFoundException e) {
      log.debug("Parameter to be deleted doesn't exist: {}", request.name());
    } catch (SdkException e) {
      throw new SecureStoreServiceException("Failed to delete secret: key = " + request.name()
        + ", error = " + e.getMessage(), e);
    }
  }
}
