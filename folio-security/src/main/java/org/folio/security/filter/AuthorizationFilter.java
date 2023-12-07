package org.folio.security.filter;

import static org.folio.common.utils.OkapiHeaders.AUTHORIZATION;
import static org.folio.common.utils.OkapiHeaders.TOKEN;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.security.exception.NotAuthorizedException;
import org.folio.security.exception.TokenMismatchException;
import org.folio.security.service.AuthorizationService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@Log4j2
@RequiredArgsConstructor
public class AuthorizationFilter extends OncePerRequestFilter {

  private final AuthorizationService authorizationService;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws IOException, ServletException {
    var okapiToken = request.getHeader(TOKEN);
    var authToken = Optional.ofNullable(request.getHeader(AUTHORIZATION)).orElse(okapiToken);

    if (authToken == null) {
      throw new NotAuthorizedException("Failed to find auth token in request.");
    }

    authToken = trimTokenBearer(authToken);

    if (okapiToken != null && !authToken.equals(okapiToken)) {
      throw new TokenMismatchException("X-Okapi-Token is not equal to Authorization token");
    }

    var auth = authorizationService.authorize(request, authToken);
    SecurityContextHolder.getContext().setAuthentication(auth);

    filterChain.doFilter(request, response);
  }

  private static String trimTokenBearer(String token) {
    return StringUtils.removeStartIgnoreCase(token, "Bearer ");
  }
}
