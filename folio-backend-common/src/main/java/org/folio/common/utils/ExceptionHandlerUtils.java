package org.folio.common.utils;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.folio.common.domain.model.error.ErrorCode.VALIDATION_ERROR;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.folio.common.domain.model.error.Error;
import org.folio.common.domain.model.error.ErrorCode;
import org.folio.common.domain.model.error.ErrorResponse;
import org.folio.common.domain.model.error.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@UtilityClass
public class ExceptionHandlerUtils {

  public static ErrorResponse buildValidationError(Exception exception, List<Parameter> parameters) {
    return buildErrorResponse(exception, parameters, VALIDATION_ERROR);
  }

  public static ErrorResponse buildErrorResponse(Exception exception, List<Parameter> parameters, ErrorCode code) {
    var error = new Error()
      .type(exception.getClass().getSimpleName())
      .code(code)
      .message(exception.getMessage())
      .parameters(isNotEmpty(parameters) ? parameters : null);
    return new ErrorResponse().errors(List.of(error)).totalRecords(1);
  }

  public static ResponseEntity<ErrorResponse> buildResponseEntity(
    Throwable exception, HttpStatus status, ErrorCode code) {

    var errorResponse = new ErrorResponse()
      .errors(List.of(new Error()
        .message(exception.getMessage())
        .type(exception.getClass().getSimpleName())
        .code(code)))
      .totalRecords(1);

    return buildResponseEntity(errorResponse, status);
  }

  public static ResponseEntity<ErrorResponse> buildResponseEntity(ErrorResponse errorResponse, HttpStatus status) {
    return ResponseEntity.status(status).body(errorResponse);
  }
}
