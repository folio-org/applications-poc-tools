package org.folio.security.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

public interface AuthorizationService {

  Authentication authorize(HttpServletRequest request, String token);
}
