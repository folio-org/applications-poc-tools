package org.folio.security.support;

import static org.folio.test.TestUtils.OBJECT_MAPPER;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestConfiguration {

  @Bean
  public ObjectMapper mapper() {
    return OBJECT_MAPPER;
  }
}
