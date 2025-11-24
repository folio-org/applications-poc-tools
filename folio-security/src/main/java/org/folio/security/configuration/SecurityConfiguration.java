package org.folio.security.configuration;

import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.folio.security.filter.ExceptionHandlerFilter;
import org.folio.security.integration.authtoken.configuration.OkapiSecurityConfiguration;
import org.folio.security.integration.keycloak.configuration.KeycloakSecurityConfiguration;
import org.folio.security.service.AuthorizationService;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

@EnableWebSecurity
@RequiredArgsConstructor
@Import({KeycloakSecurityConfiguration.class, OkapiSecurityConfiguration.class})
public class SecurityConfiguration implements WebSecurityCustomizer {

  public static final String ROUTER_PREFIX_PROPERTY = "application.router.path-prefix";
  private final Environment environment;

  /**
   * Allows unauthorized requests.
   *
   * <p>This configuration allows unauthorized requests to:
   *   <ul>
   *     <li>Spring-Boot actuator endpoints</li>
   *     <li>All GET endpoints, except excluded by pattern {@code entitlements/.+/applications}</li>
   *   </ul>
   * </p>
   *
   * @param web the instance of {@link WebSecurity} to apply to customizations to
   */
  @Override
  public void customize(WebSecurity web) {
    web.ignoring()
      .requestMatchers(EndpointRequest.toAnyEndpoint())
      .requestMatchers(regexMatcher(HttpMethod.GET, getExcludedRoutesPattern()));
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

  @Bean
  public UserDetailsService noOpUserDetailsService() {
    return username -> {
      throw new UsernameNotFoundException("UserDetailsService is not used.");
    };
  }

  private String getExcludedRoutesPattern() {
    var pathPrefix = environment.getProperty(ROUTER_PREFIX_PROPERTY, "").strip();
    pathPrefix = removeEnd(removeStart(pathPrefix, "/"), "/");
    pathPrefix = pathPrefix.length() > 1 ? pathPrefix + "/" : pathPrefix;

    return "^(?!/" + pathPrefix + "entitlements/.*/applications).*$";
  }
}
