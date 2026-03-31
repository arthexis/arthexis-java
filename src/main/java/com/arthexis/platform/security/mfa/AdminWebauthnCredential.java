package com.arthexis.platform.security.mfa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_webauthn_credential")
public class AdminWebauthnCredential {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 128)
  private String username;

  @Column(name = "credential_id", nullable = false, unique = true, length = 255)
  private String credentialId;

  @Column(name = "public_key_cose", nullable = false, columnDefinition = "TEXT")
  private String publicKeyCose;

  @Column(name = "sign_count", nullable = false)
  private long signCount;

  @Column(name = "transports", length = 255)
  private String transports;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  protected AdminWebauthnCredential() {}

  public AdminWebauthnCredential(
      String username,
      String credentialId,
      String publicKeyCose,
      long signCount,
      String transports,
      Instant createdAt) {
    this.username = username;
    this.credentialId = credentialId;
    this.publicKeyCose = publicKeyCose;
    this.signCount = signCount;
    this.transports = transports;
    this.createdAt = createdAt;
  }

  public String getUsername() {
    return username;
  }

  public String getCredentialId() {
    return credentialId;
  }

  public long getSignCount() {
    return signCount;
  }

  public void markAssertion(long nextSignCount, Instant usedAt) {
    this.signCount = Math.max(this.signCount, nextSignCount);
    this.lastUsedAt = usedAt;
  }
}
