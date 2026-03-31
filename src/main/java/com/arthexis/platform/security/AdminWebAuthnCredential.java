package com.arthexis.platform.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "admin_webauthn_credentials")
public class AdminWebAuthnCredential {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String username;

  @Column(name = "credential_id", nullable = false, unique = true)
  private String credentialId;

  @Column(name = "public_key_cose", nullable = false, length = 4096)
  private String publicKeyCose;

  @Column(name = "sign_count", nullable = false)
  private long signCount;

  @Column(length = 256)
  private String transports;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_used_at")
  private Instant lastUsedAt;

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getCredentialId() {
    return credentialId;
  }

  public void setCredentialId(String credentialId) {
    this.credentialId = credentialId;
  }

  public String getPublicKeyCose() {
    return publicKeyCose;
  }

  public void setPublicKeyCose(String publicKeyCose) {
    this.publicKeyCose = publicKeyCose;
  }

  public long getSignCount() {
    return signCount;
  }

  public void setSignCount(long signCount) {
    this.signCount = signCount;
  }

  public String getTransports() {
    return transports;
  }

  public void setTransports(String transports) {
    this.transports = transports;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastUsedAt() {
    return lastUsedAt;
  }

  public void setLastUsedAt(Instant lastUsedAt) {
    this.lastUsedAt = lastUsedAt;
  }
}
