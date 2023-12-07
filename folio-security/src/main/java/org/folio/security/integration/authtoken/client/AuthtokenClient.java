package org.folio.security.integration.authtoken.client;

import static org.folio.common.utils.OkapiHeaders.MODULE_PERMISSIONS;
import static org.folio.common.utils.OkapiHeaders.PERMISSIONS_DESIRED;
import static org.folio.common.utils.OkapiHeaders.PERMISSIONS_REQUIRED;
import static org.folio.common.utils.OkapiHeaders.TENANT;
import static org.folio.common.utils.OkapiHeaders.TOKEN;
import static org.folio.common.utils.OkapiHeaders.URL;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

public interface AuthtokenClient {

  @GetMapping("{requestPath}")
  void checkAuthToken(@PathVariable String requestPath,
    @RequestHeader(PERMISSIONS_REQUIRED) String requiredPermissions,
    @RequestHeader(PERMISSIONS_DESIRED) String desiredPermissions,
    @RequestHeader(MODULE_PERMISSIONS) Map<String, List<String>> modulePermissions,
    @RequestHeader(TOKEN) String accessToken,
    @RequestHeader(TENANT) String tenantId,
    @RequestHeader(URL) String okapiUrl);
}
