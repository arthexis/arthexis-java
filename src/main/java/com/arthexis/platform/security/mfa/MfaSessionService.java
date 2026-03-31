package com.arthexis.platform.security.mfa;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class MfaSessionService {

  public static final String MFA_VERIFIED = "arthexis.mfa.verified";
  private static final Set<String> PRIVILEGED_ROLES = Set.of("ROLE_ADMIN", "ROLE_OPERATOR");

  public boolean isPrivileged(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return false;
    }
    return authentication.getAuthorities().stream()
        .map(Object::toString)
        .anyMatch(PRIVILEGED_ROLES::contains);
  }

  public boolean isMfaVerified(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    return session != null && Boolean.TRUE.equals(session.getAttribute(MFA_VERIFIED));
  }

  public void markVerified(HttpServletRequest request) {
    request.getSession(true).setAttribute(MFA_VERIFIED, Boolean.TRUE);
  }
}
