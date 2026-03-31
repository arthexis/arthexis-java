package com.arthexis.platform.security;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class MfaService {

  static final String STEP_UP_HEADER = "X-Step-Up-Token";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final AdminWebAuthnCredentialRepository webAuthnCredentialRepository;
  private final AdminTotpFactorRepository totpFactorRepository;
  private final AdminStepUpSessionRepository stepUpSessionRepository;
  private final Map<String, String> pendingChallenges = new ConcurrentHashMap<>();

  public MfaService(
      AdminWebAuthnCredentialRepository webAuthnCredentialRepository,
      AdminTotpFactorRepository totpFactorRepository,
      AdminStepUpSessionRepository stepUpSessionRepository) {
    this.webAuthnCredentialRepository = webAuthnCredentialRepository;
    this.totpFactorRepository = totpFactorRepository;
    this.stepUpSessionRepository = stepUpSessionRepository;
  }

  public String beginWebAuthnRegistration(String username) {
    String challenge = randomBase64(32);
    pendingChallenges.put("reg:" + username, challenge);
    return challenge;
  }

  public void finishWebAuthnRegistration(WebAuthnRegistrationFinishRequest request) {
    assertChallenge("reg:" + request.username(), request.challenge());
    AdminWebAuthnCredential credential = new AdminWebAuthnCredential();
    credential.setUsername(request.username());
    credential.setCredentialId(request.credentialId());
    credential.setPublicKeyCose(request.publicKeyCose());
    credential.setSignCount(request.signCount());
    credential.setTransports(request.transports());
    credential.setCreatedAt(Instant.now());
    webAuthnCredentialRepository.save(credential);
  }

  public WebAuthnAssertionOptions beginWebAuthnAssertion(String username) {
    String challenge = randomBase64(32);
    pendingChallenges.put("assert:" + username, challenge);
    List<String> credentials =
        webAuthnCredentialRepository.findByUsername(username).stream()
            .map(AdminWebAuthnCredential::getCredentialId)
            .toList();
    return new WebAuthnAssertionOptions(challenge, credentials);
  }

  public String finishWebAuthnAssertion(WebAuthnAssertionFinishRequest request) {
    assertChallenge("assert:" + request.username(), request.challenge());
    AdminWebAuthnCredential credential =
        webAuthnCredentialRepository
            .findByUsernameAndCredentialId(request.username(), request.credentialId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown WebAuthn credential"));
    credential.setSignCount(Math.max(credential.getSignCount(), request.signCount()));
    credential.setLastUsedAt(Instant.now());
    webAuthnCredentialRepository.save(credential);
    return issueStepUpToken(request.username(), "webauthn");
  }

  public TotpEnrollment enrollTotp(String username) {
    byte[] bytes = new byte[20];
    SECURE_RANDOM.nextBytes(bytes);
    String secret = Base64.getEncoder().withoutPadding().encodeToString(bytes);
    AdminTotpFactor factor = totpFactorRepository.findByUsername(username).orElseGet(AdminTotpFactor::new);
    factor.setUsername(username);
    factor.setSecret(secret);
    factor.setEnabled(false);
    factor.setEnrolledAt(Instant.now());
    factor.setVerifiedAt(null);
    totpFactorRepository.save(factor);
    return new TotpEnrollment(secret, "otpauth://totp/Arthexis:" + username + "?secret=" + secret);
  }

  public String verifyTotp(String username, String code) {
    AdminTotpFactor factor =
        totpFactorRepository
            .findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("TOTP factor is not enrolled"));
    if (!matchesTotp(factor.getSecret(), code, Instant.now())) {
      throw new IllegalArgumentException("Invalid TOTP code");
    }
    factor.setEnabled(true);
    factor.setVerifiedAt(Instant.now());
    totpFactorRepository.save(factor);
    return issueStepUpToken(username, "totp");
  }

  public boolean hasValidStepUp(String username, String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    Optional<AdminStepUpSession> session = stepUpSessionRepository.findByTokenAndUsername(token, username);
    if (session.isEmpty()) {
      return false;
    }
    return session.get().getExpiresAt().isAfter(Instant.now());
  }

  private String issueStepUpToken(String username, String factorType) {
    stepUpSessionRepository.deleteByExpiresAtBefore(Instant.now());
    AdminStepUpSession session = new AdminStepUpSession();
    session.setToken(HexFormat.of().formatHex(randomBytes(32)));
    session.setUsername(username);
    session.setFactorType(factorType);
    session.setCreatedAt(Instant.now());
    session.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
    stepUpSessionRepository.save(session);
    return session.getToken();
  }

  private void assertChallenge(String key, String providedChallenge) {
    String expected = pendingChallenges.remove(key);
    if (expected == null || !expected.equals(providedChallenge)) {
      throw new IllegalArgumentException("Challenge is invalid or expired");
    }
  }

  private static String randomBase64(int bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(bytes));
  }

  private static byte[] randomBytes(int size) {
    byte[] value = new byte[size];
    SECURE_RANDOM.nextBytes(value);
    return value;
  }

  private boolean matchesTotp(String secret, String inputCode, Instant now) {
    for (int skew = -1; skew <= 1; skew++) {
      String expected = computeTotp(secret, now.plusSeconds(skew * 30L));
      if (expected.equals(inputCode)) {
        return true;
      }
    }
    return false;
  }

  private String computeTotp(String secret, Instant instant) {
    try {
      byte[] key = Base64.getDecoder().decode(secret);
      long counter = instant.getEpochSecond() / 30L;
      ByteBuffer message = ByteBuffer.allocate(8).putLong(counter);
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      byte[] hash = mac.doFinal(message.array());
      int offset = hash[hash.length - 1] & 0x0F;
      int binary =
          ((hash[offset] & 0x7F) << 24)
              | ((hash[offset + 1] & 0xFF) << 16)
              | ((hash[offset + 2] & 0xFF) << 8)
              | (hash[offset + 3] & 0xFF);
      int otp = binary % 1_000_000;
      return String.format("%06d", otp);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to compute TOTP", ex);
    }
  }

  public record WebAuthnAssertionOptions(String challenge, List<String> allowCredentials) {}

  public record TotpEnrollment(String secret, String otpauthUri) {}
}
