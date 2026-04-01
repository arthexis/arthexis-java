package com.arthexis.platform.firmware;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "firmware_rollout_campaign")
public class FirmwareRolloutCampaign {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "campaign_id", nullable = false, unique = true, length = 64)
  private String campaignId;

  @Column(name = "tenant_id", nullable = false, length = 64)
  private String tenantId;

  @Column(name = "target_version", nullable = false, length = 64)
  private String targetVersion;

  @Column(name = "status", nullable = false, length = 32)
  private String status;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected FirmwareRolloutCampaign() {}

  public FirmwareRolloutCampaign(String campaignId, String tenantId, String targetVersion, String status) {
    this.campaignId = campaignId;
    this.tenantId = tenantId;
    this.targetVersion = targetVersion;
    this.status = status;
    this.startedAt = Instant.now();
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    if (startedAt == null) {
      startedAt = now;
    }
    createdAt = now;
  }
}
