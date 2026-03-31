package com.arthexis.platform.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "energy_account")
public class EnergyAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "account_external_id", nullable = false, unique = true, length = 128)
  private String accountExternalId;

  @Column(name = "display_name", length = 128)
  private String displayName;

  @Column(name = "email", length = 256)
  private String email;

  @Column(name = "energy_tracking_enabled", nullable = false)
  private boolean energyTrackingEnabled = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected EnergyAccount() {}

  public EnergyAccount(String accountExternalId, String displayName, String email) {
    this.accountExternalId = accountExternalId;
    this.displayName = displayName;
    this.email = email;
    this.energyTrackingEnabled = true;
  }

  public Long getId() {
    return id;
  }

  public String getAccountExternalId() {
    return accountExternalId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getEmail() {
    return email;
  }

  public boolean isEnergyTrackingEnabled() {
    return energyTrackingEnabled;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public void setEmail(String email) {
    this.email = email;
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
