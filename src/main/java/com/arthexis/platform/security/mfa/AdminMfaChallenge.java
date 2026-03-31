package com.arthexis.platform.security.mfa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_mfa_challenge")
public class AdminMfaChallenge {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 128)
  private String username;

  @Column(name = "factor_type", nullable = false, length = 64)
  private String factorType;

  @Column(nullable = false, length = 255)
  private String challenge;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  protected AdminMfaChallenge() {}

  public AdminMfaChallenge(
      String username, String factorType, String challenge, Instant expiresAt) {
    this.username = username;
    this.factorType = factorType;
    this.challenge = challenge;
    this.expiresAt = expiresAt;
  }

  public String getChallenge() {
    return challenge;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public void markUsed(Instant at) {
    this.usedAt = at;
  }
}
