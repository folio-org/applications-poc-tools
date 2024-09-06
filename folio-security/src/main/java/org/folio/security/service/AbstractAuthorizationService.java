package org.folio.security.service;

import static org.folio.security.configuration.SecurityConfiguration.ROUTER_PREFIX_PROPERTY;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Setter;
import org.folio.security.exception.RoutingEntryMatchingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.util.UrlPathHelper;

@Setter
public abstract class AbstractAuthorizationService implements AuthorizationService {

  protected UrlPathHelper urlPathHelper;
  protected Environment environment;

  /**
   * Returns request path for routing matching.
   *
   * @param request - {@link HttpServletRequest} object
   * @return - request path as {@link String} object
   */
  protected String getRequestPath(HttpServletRequest request) {
    return updatePath(urlPathHelper.getPathWithinApplication(request));
  }

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  public void setUrlPathHelper(UrlPathHelper urlPathHelper) {
    this.urlPathHelper = urlPathHelper;
  }

  @Autowired
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  private String updatePath(String path) {
    var routerPrefix = environment.getProperty(ROUTER_PREFIX_PROPERTY, "").trim();
    var prefix = routerPrefix.startsWith("/") ? routerPrefix : "/" + routerPrefix;

    if (prefix.equals("/")) {
      return path;
    }

    if (path.startsWith(prefix)) {
      return path.substring(prefix.length());
    }

    var errorMessage = String.format("Route is not found for path: '%s'", prefix);
    throw new RoutingEntryMatchingException(errorMessage);
  }
}
