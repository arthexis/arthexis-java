package com.arthexis.platform.firmware;

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
@Table(name = "firmware_rollout_status")
public class FirmwareRolloutStatus {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "station_id", nullable = false, length = 64)
  private String stationId;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(name = "campaign_id", nullable = false, length = 64)
  private String campaignId;

  @Column(name = "target_version", nullable = false, length = 64)
  private String targetVersion;

  @Column(name = "rollout_state", nullable = false, length = 32)
  private String rolloutState;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected FirmwareRolloutStatus() {}

  public FirmwareRolloutStatus(
      String stationId, String tenantId, String campaignId, String targetVersion, String rolloutState) {
    this.stationId = stationId;
    this.tenantId = tenantId;
    this.campaignId = campaignId;
    this.targetVersion = targetVersion;
    this.rolloutState = rolloutState;
  }

  public String getStationId() {
    return stationId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getCampaignId() {
    return campaignId;
  }

  public String getTargetVersion() {
    return targetVersion;
  }

  public String getRolloutState() {
    return rolloutState;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void apply(String targetVersion, String rolloutState) {
    this.targetVersion = targetVersion;
    this.rolloutState = rolloutState;
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
