package org.folio.tools.store.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.ssm.model.ParameterType.SECURE_STRING;

import org.folio.test.types.UnitTest;
import org.folio.tools.store.exception.NotFoundException;
import org.folio.tools.store.properties.AwsConfigProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;
import software.amazon.awssdk.services.ssm.model.PutParameterRequest;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AwsStoreTest {

  public static final AwsConfigProperties AWS_CONFIG_PROPERTIES =
    AwsConfigProperties.builder().region("us-east-1").useIam(true).fipsEnabled(true)
      .trustStorePath("/certificates/keystore.bcfks").trustStorePassword("secretpassword")
      .trustStoreFileType("BCFKS").build();

  @Mock
  private SsmClient ssmClient;

  @InjectMocks
  private AwsStore awsStore = AwsStore.create(AWS_CONFIG_PROPERTIES);

  @Test
  void get_positive() {
    var key = "my_key";
    var value = "my_value";
    when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(
      GetParameterResponse.builder().parameter(Parameter.builder().value(value).build()).build());

    var result = awsStore.get(key);

    assertEquals(value, result);
  }

  @Test
  void get_negative_notFound() {
    var key = "my_key";

    when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(NotFoundException.class);

    assertThrows(NotFoundException.class, () -> awsStore.get(key));
  }

  @Test
  void lookup_positive() {
    var key = "my_key";
    var value = "my_value";
    when(ssmClient.getParameter(any(GetParameterRequest.class))).thenReturn(
      GetParameterResponse.builder().parameter(Parameter.builder().value(value).build()).build());

    var result = awsStore.lookup(key);

    assertTrue(result.isPresent());
    assertEquals(value, result.get());
  }

  @Test
  void lookup_negative_notFound() {
    var key = "my_key";

    when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(NotFoundException.class);

    var result = awsStore.lookup(key);

    assertTrue(result.isEmpty());
  }

  @Test
  void getWithParameters_negative_notFound() {
    var clientId = "ditdatdot";
    var tenant = "foo";
    var user = "bar";

    when(ssmClient.getParameter(any(GetParameterRequest.class))).thenThrow(RuntimeException.class);

    assertThrows(NotFoundException.class, () -> awsStore.get(clientId, tenant, user));
  }

  @Test
  void getWithParameters_positive() {
    var clientId = "ditdatdot";
    var tenant = "foo";
    var user = "bar";
    var val = "letmein";
    var key = String.format("%s_%s_%s", clientId, tenant, user);

    var req = GetParameterRequest.builder().name(key).withDecryption(true).build();
    var resp = GetParameterResponse.builder().parameter(Parameter.builder().name(key).value(val).build()).build();
    when(ssmClient.getParameter(req)).thenReturn(resp);

    var actual = awsStore.get(clientId, tenant, user);
    assertEquals(val, actual);
  }

  @Test
  void set_positive() {
    var key = "key";
    var value = "value";
    when(ssmClient.putParameter(any(PutParameterRequest.class))).thenReturn(null);

    awsStore.set(key, value);

    verify(ssmClient).putParameter(argThat((PutParameterRequest request) ->
      key.equals(request.name()) && value.equals(request.value()) && request.type() == SECURE_STRING)
    );
  }

  @Test
  void initializeStoreWithIamDisabled_negative() {
    var props = AwsConfigProperties.builder().region("us-east-1").useIam(false).build();

    assertThrows(SdkClientException.class, () -> AwsStore.create(props));
  }
}
