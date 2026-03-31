package com.arthexis.platform.security;

import com.arthexis.platform.security.mfa.StepUpAuthenticationFilter;
import java.util.Arrays;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain apiSecurityFilterChain(
      HttpSecurity http, Environment environment, StepUpAuthenticationFilter stepUpAuthenticationFilter)
      throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/auth/mfa/**")
                    .hasAnyRole("ADMIN", "OPERATOR")
                    .requestMatchers("/ws/admin/**")
                    .hasAnyRole("ADMIN", "OPERATOR")
                    .requestMatchers("/cp/charging/**")
                    .hasAnyRole("CP_CUSTOMER", "ADMIN", "OPERATOR")
                    .requestMatchers(HttpMethod.POST, "/admin/**")
                    .hasAnyRole("ADMIN", "OPERATOR")
                    .requestMatchers(HttpMethod.PUT, "/admin/**")
                    .hasAnyRole("ADMIN", "OPERATOR")
                    .requestMatchers(HttpMethod.PATCH, "/admin/**")
                    .hasAnyRole("ADMIN", "OPERATOR")
                    .requestMatchers(HttpMethod.DELETE, "/admin/**")
                    .hasAnyRole("ADMIN", "OPERATOR")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions ->
                exceptions.authenticationEntryPoint(
                    (request, response, authException) -> {
                      response.setStatus(401);
                      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                      response.getWriter().write("{\"error\":\"unauthorized\"}");
                    }));

    configureAuthMechanisms(http, environment);
    http.addFilterAfter(stepUpAuthenticationFilter, BasicAuthenticationFilter.class);
    return http.build();
  }

  private void configureAuthMechanisms(HttpSecurity http, Environment environment) throws Exception {
    String mode = environment.getProperty("arthexis.security.auth.mode", inferMode(environment));
    boolean jwtConfigured =
        StringUtils.hasText(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"));
    switch (mode) {
      case "basic" -> http.httpBasic(Customizer.withDefaults());
      case "jwt" -> {
        if (jwtConfigured) {
          http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        }
      }
      case "basic_and_jwt" -> {
        http.httpBasic(Customizer.withDefaults());
        if (jwtConfigured) {
          http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        }
      }
      default -> throw new IllegalStateException("Unsupported auth mode: " + mode);
    }
  }

  private String inferMode(Environment environment) {
    Set<String> profiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
    if (profiles.contains("prod") || profiles.contains("staging")) {
      return "jwt";
    }
    return "basic";
  }
}
