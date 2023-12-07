package org.folio.test.extensions.impl;

import static org.folio.test.TestUtils.asJsonString;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.folio.test.TestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;

@Log4j2
public class WireMockAdminClient {

  private final URI wiremockUri;
  private final HttpClient httpClient;

  public WireMockAdminClient(String wiremockUrl) throws URISyntaxException {
    this.wiremockUri = new URI(wiremockUrl);
    this.httpClient = HttpClient.newBuilder().build();
  }

  public String getWireMockUrl() {
    return wiremockUri.toString();
  }

  public int getWireMockPort() {
    return wiremockUri.getPort();
  }

  public void resetAll() {
    HttpRequest request = requestTo("/__admin/reset")
      .POST(BodyPublishers.noBody())
      .build();

    var response = send(request, BodyHandlers.discarding());

    if (HttpStatusCode.valueOf(response.statusCode()).isError()) {
      log.info("Reset operation failed: status = {}", response.statusCode());
      throw new WireMockException("Failed to reset server");
    }
  }

  public void addStubMapping(String stubMapping) {
    HttpRequest request = requestTo("/__admin/mappings")
      .headers("Content-Type", "application/json")
      .POST(BodyPublishers.ofString(stubMapping))
      .build();

    var response = send(request, BodyHandlers.ofString());

    if (HttpStatusCode.valueOf(response.statusCode()).isError()) {
      log.info("Stub posting failed: status = {}. {}", response.statusCode(), response.body());
      throw new WireMockException("Failed to add stub mapping:\n" + stubMapping);
    }
  }

  public int requestCount(RequestCriteria criteria) {
    HttpRequest request = requestTo("/__admin/requests/count")
      .headers("Content-Type", "application/json")
      .POST(BodyPublishers.ofString(asJsonString(criteria)))
      .build();

    var response = send(request, BodyHandlers.ofString());

    if (HttpStatusCode.valueOf(response.statusCode()).isError()) {
      log.info("Request counting failed: status = {}", response.statusCode());
      throw new WireMockException("Failed to count requests matching criteria: " + criteria);
    } else {
      var count = TestUtils.parse(response.body(), RequestCount.class).getCount();
      log.debug("Request count matching criteria: count = {}, criteria = {}", count, criteria);

      return count;
    }
  }

  public RequestList unmatchedRequests() {
    HttpRequest request = requestTo("/__admin/requests/unmatched").GET().build();

    var response = send(request, BodyHandlers.ofString());

    if (HttpStatusCode.valueOf(response.statusCode()).isError()) {
      log.info("Unmatched request retrieval failed: status = {}", response.statusCode());
      throw new WireMockException("Failed to get unmatched requests");
    } else {
      var requests = TestUtils.parse(response.body(), RequestList.class);
      log.debug("Unmatched requests: {}", requests.getRequests());

      return requests;
    }
  }

  private HttpRequest.Builder requestTo(String endpoint) {
    return HttpRequest.newBuilder()
      .uri(getUri(endpoint))
      .version(HttpClient.Version.HTTP_1_1);
  }

  private URI getUri(String endpoint) {
    try {
      return new URI(getWireMockUrl() + endpoint);
    } catch (URISyntaxException e) {
      throw new WireMockException(e);
    }
  }

  private <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
    try {
      return httpClient.send(request, responseBodyHandler);
    } catch (IOException | InterruptedException e) {
      throw new WireMockException(e);
    }
  }

  @Value
  public static final class RequestCriteria {

    String url;
    String urlPattern;
    String urlPath;
    String urlPathPattern;
    @JsonSerialize(using = HttpMethodSerializer.class)
    @JsonDeserialize(using = HttpMethodDeserializer.class)
    HttpMethod method;

    @Builder
    private RequestCriteria(String url, String urlPattern, String urlPath, String urlPathPattern, HttpMethod method) {
      var bitSum = bit(url) + bit(urlPattern) + bit(urlPath) + bit(urlPathPattern);
      if (bitSum > 1) {
        throw new IllegalArgumentException("Only one of url, urlPattern, urlPath or urlPathPattern may be specified");
      }

      this.url = url;
      this.urlPattern = urlPattern;
      this.urlPath = urlPath;
      this.urlPathPattern = urlPathPattern;
      this.method = method;
    }

    private static int bit(String s) {
      return s == null ? 0 : 1;
    }
  }

  @Data
  public static class RequestCount {

    private int count;
  }

  @Data
  public static class RequestList {

    private List<Request> requests;
  }

  @Data
  public static class Request {

    private String url;
    @JsonSerialize(using = HttpMethodSerializer.class)
    @JsonDeserialize(using = HttpMethodDeserializer.class)
    private HttpMethod method;
  }

  private static class HttpMethodSerializer extends StdScalarSerializer<HttpMethod> {

    HttpMethodSerializer() {
      super(HttpMethod.class);
    }

    @Override
    public void serialize(HttpMethod value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeString(value.name());
    }
  }

  private static class HttpMethodDeserializer extends StdScalarDeserializer<HttpMethod> {

    HttpMethodDeserializer() {
      super(HttpMethod.class);
    }

    @Override
    public HttpMethod deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      if (p.hasToken(JsonToken.VALUE_STRING)) {
        var s = p.getText();
        return HttpMethod.valueOf(s);
      }
      throw new JsonParseException(p, "Cannot deserialize HttpMethod from value: " + p.getValueAsString());
    }
  }
}
