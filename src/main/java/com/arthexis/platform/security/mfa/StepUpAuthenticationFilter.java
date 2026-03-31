package com.arthexis.platform.security.mfa;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class StepUpAuthenticationFilter extends OncePerRequestFilter {

  private final MfaSessionService mfaSessionService;

  public StepUpAuthenticationFilter(MfaSessionService mfaSessionService) {
    this.mfaSessionService = mfaSessionService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!mfaSessionService.isPrivileged(authentication) || !requiresStepUp(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    if (mfaSessionService.isMfaVerified(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
        .getWriter()
        .write(
            "{\"error\":\"mfa_required\",\"message\":\"A verified second factor is required for this route\"}");
  }

  private boolean requiresStepUp(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path.startsWith("/ws/admin/")) {
      return true;
    }
    if (path.startsWith("/admin/commands") || path.startsWith("/api/admin/commands")) {
      return true;
    }
    return path.startsWith("/admin/")
        && Set.of("POST", "PUT", "PATCH", "DELETE").contains(request.getMethod());
  }
}
