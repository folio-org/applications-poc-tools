package org.folio.tools.apisix.client;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Iterator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.folio.tools.apisix.model.ApisixRoute;
import org.folio.tools.apisix.model.ApisixService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PatchExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange
public interface ApisixAdminClient {

  /**
   * Retrieves an APISIX service by its id.
   *
   * @param serviceId - APISIX service id
   * @return retrieved {@link ApisixService} wrapped in an {@link ApisixEntry}
   */
  @GetExchange("/apisix/admin/services/{serviceId}")
  ApisixEntry<ApisixService> getService(@PathVariable("serviceId") String serviceId);

  /**
   * Creates or updates an APISIX service by its id.
   *
   * @param serviceId - APISIX service id
   * @param service - service descriptor
   * @return upserted {@link ApisixService} wrapped in an {@link ApisixEntry}
   */
  @PutExchange("/apisix/admin/services/{serviceId}")
  ApisixEntry<ApisixService> upsertService(
    @PathVariable("serviceId") String serviceId,
    @RequestBody ApisixService service);

  /**
   * Deletes an APISIX service by its id.
   *
   * @param serviceId - APISIX service id
   */
  @DeleteExchange("/apisix/admin/services/{serviceId}")
  void deleteService(@PathVariable("serviceId") String serviceId);

  /**
   * Retrieves an APISIX route by its id.
   *
   * @param routeId - APISIX route id
   * @return retrieved {@link ApisixRoute} wrapped in an {@link ApisixEntry}
   */
  @GetExchange("/apisix/admin/routes/{routeId}")
  ApisixEntry<ApisixRoute> getRoute(@PathVariable("routeId") String routeId);

  /**
   * Creates or updates an APISIX route by its id.
   *
   * @param routeId - APISIX route id
   * @param route - route descriptor
   * @return upserted {@link ApisixRoute} wrapped in an {@link ApisixEntry}
   */
  @PutExchange("/apisix/admin/routes/{routeId}")
  ApisixEntry<ApisixRoute> upsertRoute(@PathVariable("routeId") String routeId, @RequestBody ApisixRoute route);

  /**
   * Partially updates an APISIX route by its id; only the non-null fields of the body are merged into the stored
   * route, so fields outside this client's model survive.
   *
   * @param routeId - APISIX route id
   * @param route - route fields to merge
   * @return updated {@link ApisixRoute} wrapped in an {@link ApisixEntry}
   */
  @PatchExchange("/apisix/admin/routes/{routeId}")
  ApisixEntry<ApisixRoute> patchRoute(@PathVariable("routeId") String routeId, @RequestBody ApisixRoute route);

  /**
   * Deletes an APISIX route by its id.
   *
   * @param routeId - APISIX route id
   */
  @DeleteExchange("/apisix/admin/routes/{routeId}")
  void deleteRoute(@PathVariable("routeId") String routeId);

  /**
   * Retrieves a page of APISIX routes.
   *
   * @param page - 1-based page number
   * @param pageSize - page size (10-500)
   * @return page of {@link ApisixRoute} objects
   */
  @GetExchange("/apisix/admin/routes")
  ApisixResultList<ApisixRoute> getRoutes(@RequestParam("page") int page, @RequestParam("page_size") int pageSize);

  /**
   * Single-resource wrapper of the APISIX Admin API (etcd-backed key/value envelope).
   *
   * @param <T> - generic type for the wrapped value
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  class ApisixEntry<T> {

    /**
     * The etcd key of the resource.
     */
    private String key;

    /**
     * The resource value.
     */
    private T value;
  }

  /**
   * Result list wrapper of the APISIX Admin API paged list endpoints.
   *
   * @param <T> - generic type for result list values
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  class ApisixResultList<T> implements Iterable<ApisixEntry<T>> {

    /**
     * Total number of resources.
     */
    private Integer total;

    /**
     * List with result objects.
     */
    private List<ApisixEntry<T>> list;

    @Override
    public Iterator<ApisixEntry<T>> iterator() {
      return emptyIfNull(list).iterator();
    }
  }
}
