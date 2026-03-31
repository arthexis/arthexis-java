package com.arthexis.platform.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "rfid_card")
public class RfidCard {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "card_uid", nullable = false, unique = true, length = 128)
  private String cardUid;

  @Enumerated(EnumType.STRING)
  @Column(name = "auth_mode", nullable = false, length = 32)
  private RfidAuthMode authMode = RfidAuthMode.DIRECT;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "energy_account_id")
  private EnergyAccount energyAccount;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Column(name = "link_approval_required", nullable = false)
  private boolean linkApprovalRequired;

  @Column(name = "link_approved", nullable = false)
  private boolean linkApproved = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected RfidCard() {}

  public RfidCard(String cardUid, RfidAuthMode authMode) {
    this.cardUid = cardUid;
    this.authMode = authMode;
  }

  public Long getId() {
    return id;
  }

  public String getCardUid() {
    return cardUid;
  }

  public RfidAuthMode getAuthMode() {
    return authMode;
  }

  public EnergyAccount getEnergyAccount() {
    return energyAccount;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isLinkApprovalRequired() {
    return linkApprovalRequired;
  }

  public boolean isLinkApproved() {
    return linkApproved;
  }

  public void linkToAccount(EnergyAccount account, boolean approvalRequired) {
    this.energyAccount = account;
    this.linkApprovalRequired = approvalRequired;
    this.linkApproved = !approvalRequired;
  }

  public void approveLink() {
    this.linkApproved = true;
  }

  public void setAuthMode(RfidAuthMode authMode) {
    this.authMode = authMode;
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
