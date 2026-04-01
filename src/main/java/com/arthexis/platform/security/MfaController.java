package com.arthexis.platform.security;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/security/mfa")
public class MfaController {

  private final MfaService mfaService;

  public MfaController(MfaService mfaService) {
    this.mfaService = mfaService;
  }

  @PostMapping("/webauthn/register/options")
  public Map<String, Object> beginWebAuthnRegistration(Principal principal) {
    String username = principal.getName();
    return Map.of("challenge", mfaService.beginWebAuthnRegistration(username), "username", username);
  }

  @PostMapping("/webauthn/register/finish")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void finishWebAuthnRegistration(
      Principal principal, @RequestBody WebAuthnRegistrationFinishRequest request) {
    if (!principal.getName().equals(request.username())) {
      throw new IllegalArgumentException("Username mismatch");
    }
    mfaService.finishWebAuthnRegistration(request);
  }

  @PostMapping("/webauthn/assert/options")
  public Map<String, Object> beginWebAuthnAssertion(Principal principal) {
    String username = principal.getName();
    MfaService.WebAuthnAssertionOptions options = mfaService.beginWebAuthnAssertion(username);
    return Map.of(
        "challenge", options.challenge(),
        "allowCredentials", options.allowCredentials(),
        "username", username);
  }

  @PostMapping("/webauthn/assert/finish")
  public Map<String, String> finishWebAuthnAssertion(
      Principal principal, @RequestBody WebAuthnAssertionFinishRequest request) {
    if (!principal.getName().equals(request.username())) {
      throw new IllegalArgumentException("Username mismatch");
    }
    return Map.of("stepUpToken", mfaService.finishWebAuthnAssertion(request), "tokenType", "header");
  }

  @PostMapping("/totp/enroll")
  public Map<String, String> enrollTotp(Principal principal) {
    MfaService.TotpEnrollment enrollment = mfaService.enrollTotp(principal.getName());
    return Map.of("secret", enrollment.secret(), "otpauthUri", enrollment.otpauthUri());
  }

  @PostMapping("/totp/verify")
  public Map<String, String> verifyTotp(Principal principal, @RequestBody TotpVerifyRequest request) {
    return Map.of("stepUpToken", mfaService.verifyTotp(principal.getName(), request.code()));
  }
}
