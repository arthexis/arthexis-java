package com.arthexis.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, Environment environment) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health/**", "/actuator/info")
                    .permitAll()
                    .requestMatchers("/ws/admin/**")
                    .hasAnyRole("ADMIN", "OPERATOR")
                    .requestMatchers("/cp/charging/**")
                    .hasAnyRole("CP_CUSTOMER", "ADMIN", "OPERATOR")
                    .anyRequest()
                    .authenticated())
        .httpBasic(Customizer.withDefaults());

    if (StringUtils.hasText(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))) {
      http.oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
    }

    return http.build();
  }
}
