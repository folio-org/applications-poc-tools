package org.folio.tools.kong.client;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Iterator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.tools.kong.model.Route;
import org.folio.tools.kong.model.Service;
import org.folio.tools.kong.service.KongGatewayService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange
public interface KongAdminClient {

  /**
   * Retrieves {@link KongGatewayService} object by its id or name.
   *
   * @param serviceIdOrName - kong service id or name
   * @return retrieved {@link Service} object
   */
  @GetExchange("/services/{serviceIdOrName}")
  Service getService(@PathVariable("serviceIdOrName") String serviceIdOrName);

  /**
   * Updates a service in Kong gateway.
   *
   * @param serviceId - service name or id
   * @param service - service descriptor
   * @return created {@link Service} object
   */
  @PutExchange("/services/{serviceId}")
  Service upsertService(@PathVariable("serviceId") String serviceId, @RequestBody Service service);

  /**
   * Deletes a service in Kong gateway by its name or id.
   *
   * @param serviceId - service name or id
   */
  @DeleteExchange("/services/{serviceId}")
  void deleteService(@PathVariable("serviceId") String serviceId);

  /**
   * Creates a route for {@code serviceName} in Kong gateway.
   *
   * @param serviceId - service id or name
   * @param route - route descriptor
   * @return created {@link Route} object
   */
  @PutExchange("/services/{serviceId}/routes/{routeId}")
  Route upsertRoute(
    @PathVariable("serviceId") String serviceId,
    @PathVariable("routeId") String routeId,
    @RequestBody Route route);

  /**
   * Deletes a route by id for {@code serviceName} in Kong gateway.
   *
   * @param serviceId - service id or name
   * @param routeIdOrName - route id or name
   */
  @DeleteExchange("/services/{serviceId}/routes/{routeIdOrName}")
  void deleteRoute(@PathVariable("serviceId") String serviceId, @PathVariable("routeIdOrName") String routeIdOrName);

  /**
   * Get routes by tags.
   *
   * @param tags - list of tags to search for
   * @return List of {@link Route} objects and offset
   */
  @GetExchange("/routes")
  KongResultList<Route> getRoutesByTag(
    @RequestParam("tags") String tags,
    @RequestParam(value = "offset", required = false) String offset);

  /**
   * Get routes of a service.
   *
   * @param serviceId - Kong service ID or name
   * @return List of {@link Route} objects and offset
   */
  @GetExchange("/services/{serviceId}/routes")
  KongResultList<Route> getServiceRoutes(
    @RequestParam("serviceId") String serviceId,
    @RequestParam(value = "offset", required = false) Integer offset);

  /**
   * Result list object wrapper for get by tag endpoints.
   *
   * @param <T> - generic type for result list value
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  class KongResultList<T> implements Iterable<T> {

    /**
     * Next value identifier.
     */
    private String offset;

    /**
     * List with result objects.
     */
    private List<T> data;

    @Override
    public Iterator<T> iterator() {
      return emptyIfNull(data).iterator();
    }
  }
}
