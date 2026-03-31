package com.arthexis.platform.security.mfa;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth/mfa")
public class AdminMfaController {

  private final AdminMfaService adminMfaService;
  private final MfaSessionService mfaSessionService;

  public AdminMfaController(AdminMfaService adminMfaService, MfaSessionService mfaSessionService) {
    this.adminMfaService = adminMfaService;
    this.mfaSessionService = mfaSessionService;
  }

  @PostMapping("/webauthn/register/options")
  public WebauthnOptionsResponse webauthnRegistrationOptions(Authentication authentication) {
    String username = privilegedUsername(authentication);
    String challenge =
        adminMfaService.issueChallenge(username, MfaFactorType.WEBAUTHN_REGISTRATION, Duration.ofMinutes(5));
    return new WebauthnOptionsResponse(challenge, username, List.of());
  }

  @PostMapping("/webauthn/register/verify")
  public ResponseEntity<MfaStatusResponse> verifyWebauthnRegistration(
      Authentication authentication, @RequestBody WebauthnRegistrationVerifyRequest request) {
    String username = privilegedUsername(authentication);
    if (!adminMfaService.consumeChallenge(
        username, MfaFactorType.WEBAUTHN_REGISTRATION, request.challenge())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid or expired WebAuthn registration challenge");
    }
    adminMfaService.registerWebauthnCredential(
        username,
        request.credentialId(),
        request.publicKeyCose(),
        request.signCount(),
        request.transports());
    return ResponseEntity.ok(new MfaStatusResponse("webauthn_registered"));
  }

  @PostMapping("/webauthn/authenticate/options")
  public WebauthnOptionsResponse webauthnAssertionOptions(Authentication authentication) {
    String username = privilegedUsername(authentication);
    String challenge =
        adminMfaService.issueChallenge(
            username, MfaFactorType.WEBAUTHN_AUTHENTICATION, Duration.ofMinutes(3));
    return new WebauthnOptionsResponse(
        challenge, username, adminMfaService.webauthnCredentialsForUser(username));
  }

  @PostMapping("/webauthn/authenticate/verify")
  public MfaStatusResponse verifyWebauthnAssertion(
      Authentication authentication,
      HttpServletRequest request,
      @RequestBody WebauthnAssertionVerifyRequest payload) {
    String username = privilegedUsername(authentication);
    boolean validChallenge =
        adminMfaService.consumeChallenge(
            username, MfaFactorType.WEBAUTHN_AUTHENTICATION, payload.challenge());
    boolean validAssertion =
        adminMfaService.verifyWebauthnAssertion(username, payload.credentialId(), payload.signCount());
    if (!validChallenge || !validAssertion) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WebAuthn assertion failed");
    }
    mfaSessionService.markVerified(request);
    return new MfaStatusResponse("mfa_verified");
  }

  @PostMapping("/totp/enroll")
  public TotpEnrollmentResponse enrollTotp(Authentication authentication) {
    String username = privilegedUsername(authentication);
    AdminMfaService.TotpEnrollment enrollment = adminMfaService.enrollTotp(username, "Arthexis");
    return new TotpEnrollmentResponse(
        enrollment.secret(), enrollment.otpauthUri(), enrollment.alreadyEnabled());
  }

  @PostMapping("/totp/verify-enrollment")
  public MfaStatusResponse verifyTotpEnrollment(
      Authentication authentication, @RequestBody TotpVerifyRequest request) {
    String username = privilegedUsername(authentication);
    if (!adminMfaService.verifyTotpEnrollment(username, request.code())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid TOTP code");
    }
    return new MfaStatusResponse("totp_enrollment_verified");
  }

  @PostMapping("/totp/verify")
  public MfaStatusResponse verifyTotp(
      Authentication authentication,
      HttpServletRequest servletRequest,
      @RequestBody TotpVerifyRequest request) {
    String username = privilegedUsername(authentication);
    if (!adminMfaService.verifyTotpAssertion(username, request.code())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid TOTP code");
    }
    mfaSessionService.markVerified(servletRequest);
    return new MfaStatusResponse("mfa_verified");
  }

  private String privilegedUsername(Authentication authentication) {
    if (!mfaSessionService.isPrivileged(authentication)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin or operator role required");
    }
    return authentication.getName();
  }

  public record WebauthnOptionsResponse(
      String challenge, String username, List<String> allowCredentials) {}

  public record WebauthnRegistrationVerifyRequest(
      String challenge, String credentialId, String publicKeyCose, long signCount, List<String> transports) {}

  public record WebauthnAssertionVerifyRequest(String challenge, String credentialId, long signCount) {}

  public record TotpEnrollmentResponse(String secret, String otpauthUri, boolean alreadyEnabled) {}

  public record TotpVerifyRequest(String code) {}

  public record MfaStatusResponse(String status) {}
}
