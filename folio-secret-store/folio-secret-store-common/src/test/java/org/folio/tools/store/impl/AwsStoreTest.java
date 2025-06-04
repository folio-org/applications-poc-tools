package org.folio.tools.store.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.ssm.model.ParameterType.SECURE_STRING;

import java.security.Security;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.folio.test.types.UnitTest;
import org.folio.tools.store.exception.SecretNotFoundException;
import org.folio.tools.store.exception.SecureStoreServiceException;
import org.folio.tools.store.properties.AwsConfigProperties;
import org.folio.tools.store.utils.SsmClientProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DeleteParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.ParameterNotFoundException;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AwsStoreTest {

  public static final AwsConfigProperties AWS_CONFIG_PROPERTIES = AwsConfigProperties.builder()
    .region("us-east-1")
    .useIam(true)
    .fipsEnabled(true)
    .trustStorePath("src/test/resources/certificates/keystore.bcfks")
    .trustStorePassword("secretpassword")
    .trustStoreFileType("BCFKS").build();

  static {
    Security.addProvider(new BouncyCastleFipsProvider());
  }

  @InjectMocks private AwsStore awsStore;
  @Mock private SsmClient ssmClient;
  @Mock private SsmClientProvider ssmClientProvider;

  @Test
  void get_positive() {
    var key = "my_key";
    var value = "my_value";
    when(ssmClientProvider.get()).thenReturn(ssmClient);
    when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(
      GetParameterResponse.builder().parameter(Parameter.builder().value(value).build()).build());

    var result = awsStore.get(key);

    assertEquals(value, result);
  }

  @Test
  void get_negative_notFound() {
    var key = "my_key";

    when(ssmClientProvider.get()).thenReturn(ssmClient);
    when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(SecretNotFoundException.class);

    assertThrows(SecretNotFoundException.class, () -> awsStore.get(key));
  }

  @Test
  void get_negative_parameterNotFoundException() {
    var key = "key";
    when(ssmClientProvider.get()).thenReturn(ssmClient);
    when(ssmClient.getParameter(any(GetParameterRequest.class)))
      .thenThrow(ParameterNotFoundException.builder().message("not found").build());

    assertThrows(SecretNotFoundException.class, () -> awsStore.get(key));
  }

  @Test
  void get_negative_sdkException() {
    var key = "key";
    when(ssmClientProvider.get()).thenReturn(ssmClient);
    when(ssmClient.getParameter(any(GetParameterRequest.class)))
      .thenThrow(SdkException.builder().message("sdk error").build());

    var ex = assertThrows(SecureStoreServiceException.class, () -> awsStore.get(key));
    assertTrue(ex.getMessage().contains("Failed to get secret: key = key, error = sdk error"));
  }

  @Test
  void lookup_positive() {
    var key = "my_key";
    var value = "my_value";
    var ssmResponse = GetParameterResponse.builder().parameter(Parameter.builder().value(value).build()).build();
    when(ssmClientProvider.get()).thenReturn(ssmClient);
    when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(ssmResponse);

    var result = awsStore.lookup(key);

    assertTrue(result.isPresent());
    assertEquals(value, result.get());
  }

  @Test
  void lookup_negative_notFound() {
    var key = "my_key";

    when(ssmClientProvider.get()).thenReturn(ssmClient);
    when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(SecretNotFoundException.class);

    var result = awsStore.lookup(key);

    assertTrue(result.isEmpty());
  }

  @Test
  void set_positive() {
    var key = "key";
    var value = "value";
    when(ssmClientProvider.get()).thenReturn(ssmClient);
    when(ssmClient.putParameter(any(PutParameterRequest.class))).thenReturn(null);

    awsStore.set(key, value);

    verify(ssmClient).putParameter(argThat((PutParameterRequest request) ->
      key.equals(request.name()) && value.equals(request.value()) && request.type() == SECURE_STRING)
    );
  }

  @Test
  void set_negative_sdkException() {
    var key = "key";
    var value = "value";
    when(ssmClientProvider.get()).thenReturn(ssmClient);
    when(ssmClient.putParameter(any(PutParameterRequest.class)))
      .thenThrow(SdkException.builder().message("sdk error").build());

    var ex = assertThrows(SecureStoreServiceException.class, () -> awsStore.set(key, value));
    assertTrue(ex.getMessage().contains("Failed to save secret: key = key, error = sdk error"));
  }

  @Test
  void create_positive() {
    var actual = AwsStore.create(AWS_CONFIG_PROPERTIES);
    assertThat(actual).isNotNull();
  }

  @Test
  void delete_positive() {
    var key = "key";
    when(ssmClientProvider.get()).thenReturn(ssmClient);
    awsStore.delete(key);
    verify(ssmClient).deleteParameter(
      (DeleteParameterRequest) argThat(req -> key.equals(((DeleteParameterRequest) req).name())));
  }

  @Test
  void delete_negative_notFound() {
    var key = "key";
    when(ssmClientProvider.get()).thenReturn(ssmClient);
    // Simulate ParameterNotFoundException (should be caught and logged, not thrown)
    doThrow(ParameterNotFoundException.builder().message("not found").build())
      .when(ssmClient).deleteParameter((DeleteParameterRequest) any());

    awsStore.delete(key); // Should not throw
  }

  @Test
  void delete_negative_sdkException() {
    var key = "key";
    when(ssmClientProvider.get()).thenReturn(ssmClient);
    doThrow(SdkException.builder().message("sdk error").build())
      .when(ssmClient).deleteParameter((DeleteParameterRequest) any());

    var ex = assertThrows(SecureStoreServiceException.class, () -> awsStore.delete(key));
    assertTrue(ex.getMessage().contains("Failed to delete secret: key = key, error = sdk error"));
  }

  @Test
  void delete_negative_invalidKey() {
    assertThrows(IllegalArgumentException.class, () -> awsStore.delete(""));
    assertThrows(IllegalArgumentException.class, () -> awsStore.delete(null));
  }
}
