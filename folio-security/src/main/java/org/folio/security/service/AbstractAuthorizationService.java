package org.folio.security.service;

import lombok.Setter;
import org.folio.security.exception.RoutingEntryMatchingException;
import org.springframework.core.env.Environment;

@Setter
public abstract class AbstractAuthorizationService implements AuthorizationService {

  public static final String ROUTER_PREFIX_PROPERTY = "application.router.path-prefix";

  protected Environment environment;

  protected String updatePath(String path) {
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
