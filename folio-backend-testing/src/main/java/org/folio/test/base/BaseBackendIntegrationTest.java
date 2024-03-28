package org.folio.test.base;

import static org.folio.test.TestConstants.OKAPI_AUTH_TOKEN;
import static org.folio.test.TestUtils.asJsonString;
import static org.folio.test.TestUtils.readString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.extern.log4j.Log4j2;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.MethodArgumentNotValidException;

@Log4j2
public abstract class BaseBackendIntegrationTest {

  protected static final String TOKEN = "X-Okapi-Token";

  protected static MockMvc mockMvc;

  @BeforeAll
  static void setupMockMvc(@Autowired MockMvc mockMvc) {
    BaseBackendIntegrationTest.mockMvc = mockMvc;
  }

  @BeforeEach
  void setUp(TestInfo testInfo) {
    log.info("[{}.{}] is running...", this.getClass().getSimpleName(), testInfo.getDisplayName());
  }

  public static ResultActions attemptGet(String uri, Object... args) throws Exception {
    return mockMvc.perform(get(uri, args).contentType(APPLICATION_JSON).header(TOKEN, OKAPI_AUTH_TOKEN));
  }

  protected static ResultActions attemptPost(String uri, Object body, Object... args) throws Exception {
    return mockMvc.perform(post(uri, args)
      .header(TOKEN, OKAPI_AUTH_TOKEN)
      .content(asJsonString(body))
      .contentType(APPLICATION_JSON));
  }

  protected static ResultActions attemptPut(String uri, Object body, Object... args) throws Exception {
    return mockMvc.perform(put(uri, args)
      .header(TOKEN, OKAPI_AUTH_TOKEN)
      .content(asJsonString(body))
      .contentType(APPLICATION_JSON));
  }

  protected static ResultActions attemptDelete(String uri, Object... args) throws Exception {
    return mockMvc.perform(delete(uri, args)
      .header(TOKEN, OKAPI_AUTH_TOKEN)
      .contentType(APPLICATION_JSON));
  }

  public static ResultActions doGet(String uri, Object... args) throws Exception {
    return attemptGet(uri, args).andExpect(status().isOk());
  }

  public static ResultActions doGet(MockHttpServletRequestBuilder request) throws Exception {
    return mockMvc.perform(request.contentType(APPLICATION_JSON)
        .header(TOKEN, OKAPI_AUTH_TOKEN))
      .andExpect(status().isOk());
  }

  protected static ResultActions doPost(String uri, Object body, Object... args) throws Exception {
    return attemptPost(uri, body, args).andExpect(status().isCreated());
  }

  protected static ResultActions doPut(String uri, Object body, Object... args) throws Exception {
    return attemptPut(uri, body, args).andExpect(status().isOk());
  }

  protected static ResultActions doDelete(String uri, Object... args) throws Exception {
    return attemptDelete(uri, args).andExpect(status().isNoContent());
  }

  protected static ResultHandler logResponseBody() {
    return result -> log.info("[Res-Body] {}", result.getResponse().getContentAsString());
  }

  protected static String readTemplate(String filePath, Object... args) {
    String path = "json/" + filePath;

    return String.format(readString(path), args);
  }

  protected static ResultMatcher json(String path, Object... args) {
    return content().json(readTemplate(path, args));
  }

  protected static ResultMatcher[] notFoundWithMsg(String msg) {
    return Arrays.array(
      status().isNotFound(),
      jsonPath("$.errors[0].message", containsString(msg)),
      jsonPath("$.errors[0].code", is("not_found_error")));
  }

  protected static ResultMatcher[] argumentNotValidErr(String errMsg, String fieldName, Object fieldValue) {
    return validationErr(MethodArgumentNotValidException.class.getSimpleName(), errMsg, fieldName, fieldValue);
  }

  protected static ResultMatcher[] dataIntegrityErr(String errMsg) {
    return Arrays.array(
      status().isBadRequest(),
      jsonPath("$.errors[0].message", containsString(errMsg)),
      jsonPath("$.errors[0].code", is("service_error")),
      jsonPath("$.errors[0].type", is("PSQLException")),
      jsonPath("$.total_records", is(1)));
  }

  protected static ResultMatcher[] validationErr(String type, String errMsg, String fieldName,
    Object fieldValue) {
    return Arrays.array(
      status().isBadRequest(),
      jsonPath("$.errors[0].message", containsString(errMsg)),
      jsonPath("$.errors[0].code", is("validation_error")),
      jsonPath("$.errors[0].type", is(type)),
      jsonPath("$.errors[0].parameters[0].key", is(fieldName)),
      jsonPath("$.errors[0].parameters[0].value", is(String.valueOf(fieldValue))),
      jsonPath("$.total_records", is(1)));
  }

  protected static ResultMatcher[] emptyCollection(String collectionName) {
    return Arrays.array(
      jsonPath("$." + collectionName, is(empty())),
      jsonPath("$.totalRecords", is(0)));
  }
}
