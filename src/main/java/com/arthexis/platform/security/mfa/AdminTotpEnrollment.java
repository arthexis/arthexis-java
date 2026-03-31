package com.arthexis.platform.security.mfa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_totp_enrollment")
public class AdminTotpEnrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 128)
  private String username;

  @Column(nullable = false, length = 64)
  private String secret;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "verified_at")
  private Instant verifiedAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  protected AdminTotpEnrollment() {}

  public AdminTotpEnrollment(String username, String secret, Instant createdAt) {
    this.username = username;
    this.secret = secret;
    this.createdAt = createdAt;
    this.enabled = false;
  }

  public String getSecret() {
    return secret;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void markVerified(Instant at) {
    this.enabled = true;
    this.verifiedAt = at;
    this.lastUsedAt = at;
  }

  public void markUsed(Instant at) {
    this.lastUsedAt = at;
  }
}
