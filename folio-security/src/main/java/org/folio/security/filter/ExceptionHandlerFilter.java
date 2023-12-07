package org.folio.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.common.domain.model.error.ErrorCode;
import org.folio.common.domain.model.error.ErrorResponse;
import org.folio.common.utils.ExceptionHandlerUtils;
import org.folio.security.exception.ForbiddenException;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.exception.RoutingEntryMatchingException;
import org.folio.security.exception.TokenMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.filter.OncePerRequestFilter;

@Log4j2
@RequiredArgsConstructor
public class ExceptionHandlerFilter extends OncePerRequestFilter {

  private final ObjectMapper mapper;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws IOException {
    try {
      filterChain.doFilter(request, response);
    } catch (ForbiddenException exception) {
      handleException(response, exception, HttpStatus.FORBIDDEN, ErrorCode.AUTH_ERROR);
    } catch (NotAuthorizedException exception) {
      handleException(response, exception, HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_ERROR);
    } catch (TokenMismatchException exception) {
      handleException(response, exception, HttpStatus.BAD_REQUEST, ErrorCode.FOUND_ERROR);
    } catch (IllegalArgumentException exception) {
      handleException(response, exception, HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);
    } catch (RoutingEntryMatchingException exception) {
      handleException(response, exception, HttpStatus.NOT_FOUND, ErrorCode.ROUTE_NOT_FOUND_ERROR);
    } catch (Exception exception) {
      handleException(response, exception, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.UNKNOWN_ERROR);
    }
  }

  public void handleException(HttpServletResponse response, Exception exception, HttpStatus status, ErrorCode errorCode)
    throws IOException {
    log.debug("Handling filter exception", exception);
    var responseEntity = ExceptionHandlerUtils.buildResponseEntity(exception, status, errorCode);
    writeResponse(response, responseEntity);
  }

  private void writeResponse(HttpServletResponse response, ResponseEntity<ErrorResponse> responseEntity)
    throws IOException {
    response.setStatus(responseEntity.getStatusCodeValue());

    var error = responseEntity.getBody();
    if (error != null) {
      response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

      var writer = response.getWriter();
      writer.print(mapper.writeValueAsString(error));
      writer.flush();
    }
  }
}
