package org.folio.security.integration.keycloak.configuration;

/**
 * Keycloak feign client additional configuration.
 */
public class KeycloakFeignConfiguration {

  /**
   * Additional map to form-data encoder for feign client.
   *
   * <p>TODO: APPPOCTOOL-78 - Temporarily disabled due to Spring Boot 4 / OpenFeign compatibility issue.
   * Spring Cloud OpenFeign 4.3.0 hasn't been updated for Spring Boot 4's package relocation
   * (HttpMessageConverters moved from org.springframework.boot.autoconfigure.http to
   * org.springframework.boot.http.converter.autoconfigure). This encoder is used for form-data
   * in Keycloak client. Monitor https://github.com/spring-cloud/spring-cloud-openfeign for updates.</p>
   *
   * @param converters - existing {@link HttpMessageConverters} factory
   * @return configured {@link Encoder} object
   */
  // @Bean
  // Encoder feignFormEncoder(ObjectFactory<HttpMessageConverters> converters) {
  //   return new FormEncoder(new SpringEncoder(converters));
  // }
}
