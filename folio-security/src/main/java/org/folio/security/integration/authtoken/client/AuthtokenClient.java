package org.folio.security.integration.authtoken.client;

import static org.folio.common.utils.OkapiHeaders.MODULE_PERMISSIONS;
import static org.folio.common.utils.OkapiHeaders.PERMISSIONS_DESIRED;
import static org.folio.common.utils.OkapiHeaders.PERMISSIONS_REQUIRED;
import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.folio.common.utils.OkapiHeaders.TOKEN;
import static org.folio.common.utils.OkapiHeaders.URL;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface AuthtokenClient {

  @GetExchange
  void checkAuthToken(URI requestUri,
    @RequestHeader(value = PERMISSIONS_REQUIRED, required = false) String requiredPermissions,
    @RequestHeader(value = PERMISSIONS_DESIRED, required = false) String desiredPermissions,
    @RequestHeader(MODULE_PERMISSIONS) Map<String, List<String>> modulePermissions,
    @RequestHeader(TOKEN) String accessToken,
    @RequestHeader(TENANT) String tenantId,
    @RequestHeader(URL) String okapiUrl);
}
