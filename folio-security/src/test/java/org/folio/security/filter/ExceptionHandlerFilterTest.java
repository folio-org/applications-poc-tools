package org.folio.security.filter;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.folio.common.domain.model.error.ErrorCode.AUTH_ERROR;
import static org.folio.common.domain.model.error.ErrorCode.FOUND_ERROR;
import static org.folio.common.domain.model.error.ErrorCode.ROUTE_NOT_FOUND_ERROR;
import static org.folio.common.domain.model.error.ErrorCode.UNKNOWN_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Stream;
import org.folio.common.domain.model.error.Error;
import org.folio.common.domain.model.error.ErrorCode;
import org.folio.common.domain.model.error.ErrorResponse;
import org.folio.security.exception.ForbiddenException;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.exception.RoutingEntryMatchingException;
import org.folio.security.exception.TokenMismatchException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ExceptionHandlerFilterTest {

  @Mock private FilterChain filterChain;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private PrintWriter writer;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private ExceptionHandlerFilter filter;

  @Test
  void exceptionHandler_noExceptions() throws IOException {
    filter.doFilterInternal(request, response, filterChain);

    verifyNoInteractions(response);
  }

  @ParameterizedTest(name = "[{index}] {0}: {1} - {2}")
  @MethodSource("exceptionProvider")
  @DisplayName("exceptionHandler_parameterized")
  void exceptionHandler_parameterized(Exception exception, int status, ErrorCode code)
    throws IOException, ServletException {
    when(response.getWriter()).thenReturn(writer);
    doThrow(exception).when(filterChain).doFilter(any(), any());

    filter.doFilterInternal(request, response, filterChain);

    verify(response).setStatus(status);
    verify(response).setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

    var errorResponse = new ErrorResponse()
      .errors(List.of(new Error()
        .message(exception.getMessage())
        .type(exception.getClass().getSimpleName())
        .code(code)))
      .totalRecords(1);

    verify(objectMapper).writeValueAsString(errorResponse);
  }

  private static Stream<Arguments> exceptionProvider() {
    return Stream.of(
      Arguments.arguments(new ForbiddenException("test"), SC_FORBIDDEN, AUTH_ERROR),
      Arguments.arguments(new NotAuthorizedException("test"), SC_UNAUTHORIZED, AUTH_ERROR),
      Arguments.arguments(new TokenMismatchException("test"), SC_BAD_REQUEST, FOUND_ERROR),
      Arguments.arguments(new RoutingEntryMatchingException("test"), SC_NOT_FOUND, ROUTE_NOT_FOUND_ERROR),
      Arguments.arguments(new RuntimeException("test"), SC_INTERNAL_SERVER_ERROR, UNKNOWN_ERROR));
  }
}
