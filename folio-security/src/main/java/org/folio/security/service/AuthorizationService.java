package org.folio.security.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface AuthorizationService {

  /**
   * Authorizes incoming http request.
   *
   * @param request - {@link HttpServletRequest} request object
   * @param token - JWT to validate
   * @return {@link Authentication} object
   */
  Authentication authorize(HttpServletRequest request, String token);
}
