package com.arthexis.platform.users;

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
@Table(name = "users_account_metadata")
public class UserAccountMetadata {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(name = "account_id", nullable = false, length = 64)
  private String accountId;

  @Column(name = "operator_id", length = 64)
  private String operatorId;

  @Column(name = "customer_id", length = 64)
  private String customerId;

  @Column(name = "identity_state", nullable = false, length = 32)
  private String identityState;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected UserAccountMetadata() {}

  public UserAccountMetadata(String tenantId, String accountId) {
    this.tenantId = tenantId;
    this.accountId = accountId;
    this.identityState = "ACTIVE";
  }

  public Long getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getOperatorId() {
    return operatorId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public String getIdentityState() {
    return identityState;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateIdentity(String operatorId, String customerId, String identityState) {
    this.operatorId = operatorId;
    this.customerId = customerId;
    if (identityState != null && !identityState.isBlank()) {
      this.identityState = identityState;
    }
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
