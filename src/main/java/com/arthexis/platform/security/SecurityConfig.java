package com.arthexis.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfig {

  private static final String[] LOCAL_AUTH_PROFILES = {"dev", "local", "h2", "test"};

  @Bean
  SecurityFilterChain apiSecurityFilterChain(
      HttpSecurity http, Environment environment, MfaService mfaService, ObjectMapper objectMapper)
      throws Exception {
    AuthorizationManager<RequestAuthorizationContext> stepUpManager =
        (authentication, context) -> {
          Authentication auth = authentication.get();
          if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
          }
          boolean hasRole =
              auth.getAuthorities().stream()
                  .anyMatch(
                      grantedAuthority ->
                          "ROLE_ADMIN".equals(grantedAuthority.getAuthority())
                              || "ROLE_OPERATOR".equals(grantedAuthority.getAuthority()));
          if (!hasRole) {
            return new AuthorizationDecision(false);
          }
          String token = context.getRequest().getHeader(MfaService.STEP_UP_HEADER);
          return new AuthorizationDecision(mfaService.hasValidStepUp(auth.getName(), token));
        };

    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/security/mfa/**")
                    .hasAnyRole("ADMIN", "OPERATOR")
                    .requestMatchers("/ws/admin/**")
                    .access(stepUpManager)
                    .requestMatchers(HttpMethod.POST, "/admin/api/commands")
                    .access(stepUpManager)
                    .requestMatchers(HttpMethod.POST, "/admin/**")
                    .access(stepUpManager)
                    .requestMatchers(HttpMethod.PUT, "/admin/**")
                    .access(stepUpManager)
                    .requestMatchers(HttpMethod.PATCH, "/admin/**")
                    .access(stepUpManager)
                    .requestMatchers(HttpMethod.DELETE, "/admin/**")
                    .access(stepUpManager)
                    .requestMatchers("/admin/**")
                    .hasAnyRole("ADMIN", "OPERATOR")
                    .requestMatchers("/cp/charging/**")
                    .hasAnyRole("CP_CUSTOMER", "ADMIN", "OPERATOR")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exception ->
                exception.accessDeniedHandler(
                    (request, response, accessDeniedException) -> {
                      if (request.getRequestURI().startsWith("/admin")
                          || request.getRequestURI().startsWith("/ws/admin")) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(
                            response.getWriter(),
                            Map.of(
                                "error", "second_factor_required",
                                "message", "A valid step-up token is required for this action"));
                        return;
                      }
                      response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    }));

    if (StringUtils.hasText(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))) {
      http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
    }

    if (isLocalAuthProfileEnabled(environment)) {
      http.httpBasic(Customizer.withDefaults());
    }

    return http.build();
  }

  private boolean isLocalAuthProfileEnabled(Environment environment) {
    for (String profile : environment.getActiveProfiles()) {
      for (String allowed : LOCAL_AUTH_PROFILES) {
        if (allowed.equals(profile)) {
          return true;
        }
      }
    }
    return false;
  }
}
