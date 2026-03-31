package com.arthexis.platform.security.mfa;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMfaService {

  private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  private final AdminMfaChallengeRepository challengeRepository;
  private final AdminWebauthnCredentialRepository credentialRepository;
  private final AdminTotpEnrollmentRepository totpEnrollmentRepository;
  private final SecureRandom secureRandom = new SecureRandom();

  public AdminMfaService(
      AdminMfaChallengeRepository challengeRepository,
      AdminWebauthnCredentialRepository credentialRepository,
      AdminTotpEnrollmentRepository totpEnrollmentRepository) {
    this.challengeRepository = challengeRepository;
    this.credentialRepository = credentialRepository;
    this.totpEnrollmentRepository = totpEnrollmentRepository;
  }

  @Transactional
  public String issueChallenge(String username, MfaFactorType factorType, Duration ttl) {
    byte[] raw = new byte[32];
    secureRandom.nextBytes(raw);
    String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    challengeRepository.save(
        new AdminMfaChallenge(username, factorType.name(), challenge, Instant.now().plus(ttl)));
    return challenge;
  }

  @Transactional
  public boolean consumeChallenge(String username, MfaFactorType factorType, String challenge) {
    return challengeRepository
        .findByUsernameAndFactorTypeAndChallenge(username, factorType.name(), challenge)
        .filter(c -> c.getUsedAt() == null)
        .filter(c -> c.getExpiresAt().isAfter(Instant.now()))
        .map(
            c -> {
              c.markUsed(Instant.now());
              return true;
            })
        .orElse(false);
  }

  @Transactional
  public void registerWebauthnCredential(
      String username,
      String credentialId,
      String publicKeyCose,
      long signCount,
      List<String> transports) {
    String transportValue = transports == null ? null : String.join(",", transports);
    credentialRepository.save(
        new AdminWebauthnCredential(
            username, credentialId, publicKeyCose, signCount, transportValue, Instant.now()));
  }

  public List<String> webauthnCredentialsForUser(String username) {
    return credentialRepository.findByUsername(username).stream()
        .map(AdminWebauthnCredential::getCredentialId)
        .collect(Collectors.toList());
  }

  @Transactional
  public boolean verifyWebauthnAssertion(String username, String credentialId, long signCount) {
    return credentialRepository
        .findByUsernameAndCredentialId(username, credentialId)
        .map(
            credential -> {
              credential.markAssertion(signCount, Instant.now());
              return true;
            })
        .orElse(false);
  }

  @Transactional
  public TotpEnrollment enrollTotp(String username, String issuer) {
    String secret = randomBase32(32);
    AdminTotpEnrollment enrollment =
        totpEnrollmentRepository
            .findByUsername(username)
            .orElseGet(() -> new AdminTotpEnrollment(username, secret, Instant.now()));
    if (enrollment.isEnabled()) {
      return new TotpEnrollment(secret, otpauthUri(issuer, username, secret), true);
    }
    if (!enrollment.getSecret().equals(secret)) {
      enrollment = new AdminTotpEnrollment(username, secret, Instant.now());
    }
    totpEnrollmentRepository.save(enrollment);
    return new TotpEnrollment(secret, otpauthUri(issuer, username, secret), false);
  }

  @Transactional
  public boolean verifyTotpEnrollment(String username, String code) {
    return totpEnrollmentRepository
        .findByUsername(username)
        .filter(enrollment -> verifyTotpCode(enrollment.getSecret(), code))
        .map(
            enrollment -> {
              enrollment.markVerified(Instant.now());
              return true;
            })
        .orElse(false);
  }

  @Transactional
  public boolean verifyTotpAssertion(String username, String code) {
    return totpEnrollmentRepository
        .findByUsername(username)
        .filter(AdminTotpEnrollment::isEnabled)
        .filter(enrollment -> verifyTotpCode(enrollment.getSecret(), code))
        .map(
            enrollment -> {
              enrollment.markUsed(Instant.now());
              return true;
            })
        .orElse(false);
  }

  private boolean verifyTotpCode(String secret, String code) {
    if (code == null || !code.matches("\\d{6}")) {
      return false;
    }
    long nowWindow = Instant.now().getEpochSecond() / 30L;
    for (long offset = -1; offset <= 1; offset++) {
      String expected = hotp(secret, nowWindow + offset);
      if (expected.equals(code)) {
        return true;
      }
    }
    return false;
  }

  private String hotp(String base32Secret, long counter) {
    try {
      byte[] key = decodeBase32(base32Secret);
      byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      byte[] hash = mac.doFinal(msg);
      int offset = hash[hash.length - 1] & 0x0F;
      int binary =
          ((hash[offset] & 0x7F) << 24)
              | ((hash[offset + 1] & 0xFF) << 16)
              | ((hash[offset + 2] & 0xFF) << 8)
              | (hash[offset + 3] & 0xFF);
      return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("Unable to compute TOTP", ex);
    }
  }

  private String randomBase32(int chars) {
    byte[] bytes = new byte[chars];
    secureRandom.nextBytes(bytes);
    StringBuilder value = new StringBuilder(chars);
    for (int i = 0; i < chars; i++) {
      value.append(BASE32_ALPHABET.charAt(bytes[i] & 31));
    }
    return value.toString();
  }

  private byte[] decodeBase32(String input) {
    String normalized = input.replace("=", "").toUpperCase(Locale.ROOT);
    ByteBuffer buffer = ByteBuffer.allocate((normalized.length() * 5) / 8 + 8);
    int bitsLeft = 0;
    int value = 0;
    for (char c : normalized.toCharArray()) {
      int idx = BASE32_ALPHABET.indexOf(c);
      if (idx < 0) {
        continue;
      }
      value = (value << 5) | idx;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        buffer.put((byte) ((value >> (bitsLeft - 8)) & 0xFF));
        bitsLeft -= 8;
      }
    }
    byte[] out = new byte[buffer.position()];
    buffer.flip();
    buffer.get(out);
    return out;
  }

  private String otpauthUri(String issuer, String username, String secret) {
    String encIssuer =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(issuer.getBytes(StandardCharsets.UTF_8));
    return "otpauth://totp/"
        + issuer
        + ":"
        + username
        + "?secret="
        + secret
        + "&issuer="
        + issuer
        + "&algorithm=SHA1&digits=6&period=30&hint="
        + encIssuer;
  }

  public record TotpEnrollment(String secret, String otpauthUri, boolean alreadyEnabled) {}
}
