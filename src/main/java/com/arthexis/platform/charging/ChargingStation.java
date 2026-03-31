package com.arthexis.platform.charging;

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
@Table(name = "charging_station")
public class ChargingStation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "station_id", length = 64, nullable = false, unique = true)
  private String stationId;

  @Column(name = "status", length = 32, nullable = false)
  private String status;

  @Column(name = "last_seen_at")
  private Instant lastSeenAt;

  @Column(name = "display_name", length = 128)
  private String displayName;

  @Column(name = "vendor", length = 128)
  private String vendor;

  @Column(name = "model", length = 128)
  private String model;

  @Column(name = "protocol_version", length = 32)
  private String protocolVersion;

  @Column(name = "firmware_version", length = 64)
  private String firmwareVersion;

  @Column(name = "tenant_id", length = 64)
  private String tenantId;

  @Column(name = "site_id", length = 64)
  private String siteId;

  @Column(name = "enabled", nullable = false)
  private boolean enabled = true;

  @Column(name = "last_heartbeat_at")
  private Instant lastHeartbeatAt;

  @Column(name = "last_boot_at")
  private Instant lastBootAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ChargingStation() {}

  public ChargingStation(String stationId, String status) {
    this.stationId = stationId;
    this.status = status;
    Instant now = Instant.now();
    this.lastSeenAt = now;
    this.lastHeartbeatAt = now;
    this.enabled = true;
  }

  public Long getId() {
    return id;
  }

  public String getStationId() {
    return stationId;
  }

  public String getStatus() {
    return status;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getVendor() {
    return vendor;
  }

  public String getModel() {
    return model;
  }

  public String getProtocolVersion() {
    return protocolVersion;
  }

  public String getFirmwareVersion() {
    return firmwareVersion;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getSiteId() {
    return siteId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Instant getLastHeartbeatAt() {
    return lastHeartbeatAt;
  }

  public Instant getLastBootAt() {
    return lastBootAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void heartbeat(String newStatus) {
    Instant now = Instant.now();
    this.status = newStatus;
    this.lastSeenAt = now;
    this.lastHeartbeatAt = now;
  }

  public void applyAdminDetails(ChargingStationAdminDetails details) {
    if (details == null) {
      return;
    }
    this.displayName = valueOrCurrent(details.displayName(), this.displayName);
    this.vendor = valueOrCurrent(details.vendor(), this.vendor);
    this.model = valueOrCurrent(details.model(), this.model);
    this.protocolVersion = valueOrCurrent(details.protocolVersion(), this.protocolVersion);
    this.firmwareVersion = valueOrCurrent(details.firmwareVersion(), this.firmwareVersion);
    this.tenantId = valueOrCurrent(details.tenantId(), this.tenantId);
    this.siteId = valueOrCurrent(details.siteId(), this.siteId);
    if (details.enabled() != null) {
      this.enabled = details.enabled();
    }
    if (details.lastHeartbeatAt() != null) {
      this.lastHeartbeatAt = details.lastHeartbeatAt();
    }
    if (details.lastBootAt() != null) {
      this.lastBootAt = details.lastBootAt();
    }
  }

  private String valueOrCurrent(String candidate, String currentValue) {
    return candidate == null || candidate.isBlank() ? currentValue : candidate;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
