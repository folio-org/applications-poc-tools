package org.folio.security.configuration;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.security.filter.ExceptionHandlerFilter;
import org.folio.security.integration.authtoken.configuration.OkapiSecurityConfiguration;
import org.folio.security.integration.keycloak.configuration.KeycloakSecurityConfiguration;
import org.folio.security.service.AuthorizationService;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@EnableWebSecurity
@Import({KeycloakSecurityConfiguration.class, OkapiSecurityConfiguration.class})
public class SecurityConfiguration implements WebSecurityCustomizer {

  @Override
  public void customize(WebSecurity web) {
    // disable security on actuator endpoints
    web.ignoring().requestMatchers(EndpointRequest.toAnyEndpoint())
      //temporary solution for phase1
      // ui should be able to load all information about tenants, entitlements and module descriptors
      .requestMatchers(regexMatcher(HttpMethod.GET, "^(?!/entitlements/.*/applications).*$"));
  }

  @Bean
  @ConditionalOnBean(AuthorizationService.class)
  public SecurityFilterChain filterChain(HttpSecurity http, AuthorizationService authService, ObjectMapper mapper)
    throws Exception {
    return http
      .csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
      .addFilterBefore(new org.folio.security.filter.AuthorizationFilter(authService), AuthorizationFilter.class)
      .addFilterBefore(new ExceptionHandlerFilter(mapper), org.folio.security.filter.AuthorizationFilter.class)
      .build();
  }

  @Bean
  @ConditionalOnMissingBean
  public SecurityFilterChain noAuthFilterChain(HttpSecurity http) throws Exception {
    return http
      .csrf(AbstractHttpConfigurer::disable)
      .authorizeHttpRequests(auth -> auth.requestMatchers("/**").permitAll())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .build();
  }
}
