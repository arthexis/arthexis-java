package com.arthexis.platform.charging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "charging_connector_state",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_connector_state_station_evse_connector",
          columnNames = {"station_id", "evse_id", "connector_id"})
    })
public class ChargingConnectorState {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "station_id", length = 64, nullable = false)
  private String stationId;

  @Column(name = "evse_id", nullable = false)
  private int evseId;

  @Column(name = "connector_id", nullable = false)
  private int connectorId;

  @Column(name = "connector_status", length = 32, nullable = false)
  private String connectorStatus;

  @Column(name = "connector_type", length = 64)
  private String connectorType;

  @Column(name = "availability", length = 32)
  private String availability;

  @Column(name = "last_status_at", nullable = false)
  private Instant lastStatusAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected ChargingConnectorState() {}

  public ChargingConnectorState(
      String stationId,
      int evseId,
      int connectorId,
      String connectorStatus,
      String connectorType,
      String availability,
      Instant lastStatusAt) {
    this.stationId = stationId;
    this.evseId = evseId;
    this.connectorId = connectorId;
    this.connectorStatus = connectorStatus;
    this.connectorType = connectorType;
    this.availability = availability;
    this.lastStatusAt = lastStatusAt;
  }

  public Long getId() {
    return id;
  }

  public String getStationId() {
    return stationId;
  }

  public int getEvseId() {
    return evseId;
  }

  public int getConnectorId() {
    return connectorId;
  }

  public String getConnectorStatus() {
    return connectorStatus;
  }

  public String getConnectorType() {
    return connectorType;
  }

  public String getAvailability() {
    return availability;
  }

  public Instant getLastStatusAt() {
    return lastStatusAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void updateState(String status, String type, String newAvailability, Instant reportedAt) {
    this.connectorStatus = status;
    this.connectorType = type;
    this.availability = newAvailability;
    this.lastStatusAt = reportedAt;
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
    if (lastStatusAt == null) {
      lastStatusAt = now;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
