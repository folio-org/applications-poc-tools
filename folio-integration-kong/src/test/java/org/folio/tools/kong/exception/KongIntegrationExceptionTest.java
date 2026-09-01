package org.folio.tools.kong.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.folio.common.domain.model.error.Parameter;
import org.folio.common.gateway.exception.ApiGatewayIntegrationException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class KongIntegrationExceptionTest {

  private static final List<Parameter> ERRORS = List.of(new Parameter().key("key").value("value"));

  @Test
  void constructor_positive_isApiGatewayIntegrationException() {
    var exception = new KongIntegrationException("error message", ERRORS);

    assertThat(exception).isInstanceOf(ApiGatewayIntegrationException.class);
    assertThat(exception.getErrors()).isEqualTo(ERRORS);
    assertThat(exception.getMessage()).isEqualTo("error message");
  }

  @Test
  void constructor_positive_errorsAccessibleViaSupertype() {
    var cause = new RuntimeException("cause");

    ApiGatewayIntegrationException exception = new KongIntegrationException("error message", ERRORS, cause);

    assertThat(exception.getErrors()).isEqualTo(ERRORS);
    assertThat(exception.getCause()).isEqualTo(cause);
  }
}
