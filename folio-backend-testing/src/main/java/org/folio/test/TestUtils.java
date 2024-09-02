package org.folio.test;

import static org.springframework.test.util.ReflectionTestUtils.getField;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.springframework.test.web.servlet.MvcResult;

@Log4j2
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestUtils {

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .setSerializationInclusion(Include.NON_EMPTY)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  @SneakyThrows
  public static <T> T parse(String value, Class<T> type) {
    return OBJECT_MAPPER.readValue(value, type);
  }

  @SneakyThrows
  public static <T> T parse(String value, TypeReference<T> type) {
    return OBJECT_MAPPER.readValue(value, type);
  }

  @SneakyThrows
  public static <T> T parseResponse(MvcResult result, Class<T> type) {
    return OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), type);
  }

  @SneakyThrows
  public static <T> T parseResponse(MvcResult result, TypeReference<T> type) {
    return OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), type);
  }

  @SneakyThrows
  public static String readString(String path) {
    try (var resource = readStream(path)) {
      return IOUtils.toString(resource, StandardCharsets.UTF_8);
    }
  }

  @SneakyThrows
  public static InputStream readStream(String path) {
    return TestUtils.class.getClassLoader().getResourceAsStream(path);
  }

  @SneakyThrows
  public static File readToFile(String path, String fileName, String fileExtension) {
    log.info("Loading test file [path: {}, fileName: {}]", path, fileName);
    var file = File.createTempFile(fileName, fileExtension);
    file.deleteOnExit();
    FileUtils.copyInputStreamToFile(readStream(path), file);
    return file;
  }

  public static void verifyNoMoreInteractions(Object testClassInstance) {
    var declaredFields = testClassInstance.getClass().getDeclaredFields();
    var mocks = Arrays.stream(declaredFields)
      .filter(field -> field.getAnnotation(Mock.class) != null || field.getAnnotation(Spy.class) != null)
      .map(field -> getField(testClassInstance, field.getName()))
      .toArray();

    Mockito.verifyNoMoreInteractions(mocks);
  }
}
