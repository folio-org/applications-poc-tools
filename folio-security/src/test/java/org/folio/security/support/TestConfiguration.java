package org.folio.security.support;

import static org.folio.test.TestUtils.OBJECT_MAPPER;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class TestConfiguration {

  @Bean
  public ObjectMapper mapper() {
    return OBJECT_MAPPER;
  }
}
