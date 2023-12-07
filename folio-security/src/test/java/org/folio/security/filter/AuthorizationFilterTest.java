package org.folio.security.filter;

import static org.folio.common.utils.OkapiHeaders.AUTHORIZATION;
import static org.folio.common.utils.OkapiHeaders.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.folio.security.exception.TokenMismatchException;
import org.folio.security.service.AuthorizationService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@UnitTest
@ExtendWith(MockitoExtension.class)
class AuthorizationFilterTest {

  private static final String OKAPI_TOKEN = "test";
  private static final String BEARER_OKAPI_TOKEN = "Bearer " + OKAPI_TOKEN;
  @Mock private FilterChain filterChain;
  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private Authentication authentication;
  @Mock private AuthorizationService authorizationService;

  @InjectMocks private AuthorizationFilter filter;

  @AfterEach
  void cleanUp() {
    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  void filter_positive_okapiHeader() throws ServletException, IOException {
    when(request.getHeader(TOKEN)).thenReturn(OKAPI_TOKEN);
    when(authorizationService.authorize(any(), any())).thenReturn(authentication);

    filter.doFilterInternal(request, response, filterChain);

    verify(authorizationService).authorize(request, OKAPI_TOKEN);
    assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void filter_positive_authHeader() throws ServletException, IOException {
    when(request.getHeader(TOKEN)).thenReturn(null);
    when(request.getHeader(AUTHORIZATION)).thenReturn(BEARER_OKAPI_TOKEN);
    when(authorizationService.authorize(any(), any())).thenReturn(authentication);

    filter.doFilterInternal(request, response, filterChain);

    verify(authorizationService).authorize(request, OKAPI_TOKEN);
    assertEquals(authentication, SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void filter_negative_tokenMismatch() {
    when(request.getHeader(TOKEN)).thenReturn("another value");
    when(request.getHeader(AUTHORIZATION)).thenReturn(BEARER_OKAPI_TOKEN);

    assertThrows(TokenMismatchException.class, () -> filter.doFilterInternal(request, response, filterChain));

    verifyNoInteractions(authorizationService);
    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void filter_negative_exceptionThrown() {
    when(request.getHeader(TOKEN)).thenReturn(OKAPI_TOKEN);
    when(authorizationService.authorize(any(), any())).thenThrow(RuntimeException.class);

    assertThrows(RuntimeException.class, () -> filter.doFilterInternal(request, response, filterChain));

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
