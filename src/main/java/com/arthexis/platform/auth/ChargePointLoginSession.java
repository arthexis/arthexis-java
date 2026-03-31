package com.arthexis.platform.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "charge_point_login_session")
public class ChargePointLoginSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "login_token", nullable = false, unique = true, length = 128)
  private String loginToken;

  @Column(name = "station_id", nullable = false, length = 64)
  private String stationId;

  @Column(name = "card_uid", nullable = false, length = 128)
  private String cardUid;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private LoginSessionStatus status = LoginSessionStatus.PENDING;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "account_external_id", length = 128)
  private String accountExternalId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ChargePointLoginSession() {}

  public ChargePointLoginSession(String loginToken, String stationId, String cardUid, Instant expiresAt) {
    this.loginToken = loginToken;
    this.stationId = stationId;
    this.cardUid = cardUid;
    this.expiresAt = expiresAt;
  }

  public String getLoginToken() {
    return loginToken;
  }

  public String getStationId() {
    return stationId;
  }

  public String getCardUid() {
    return cardUid;
  }

  public LoginSessionStatus getStatus() {
    return status;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markCompleted(String accountExternalId) {
    this.accountExternalId = accountExternalId;
    this.status = LoginSessionStatus.COMPLETED;
  }

  public void markExpired() {
    this.status = LoginSessionStatus.EXPIRED;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
