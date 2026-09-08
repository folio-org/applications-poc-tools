package org.folio.tools.kong.utls;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.folio.common.domain.model.RoutingEntry;
import org.folio.common.gateway.utils.GatewayRouteUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RoutingEntryUtils {

  /**
   * Provides a list with http method names for {@link RoutingEntry} object.
   *
   * @param re - routing entry to process
   * @return {@link List} with {@link String} method names
   */
  public static List<String> getMethods(RoutingEntry re) {
    return GatewayRouteUtils.getMethods(re);
  }
}
