package com.arthexis.platform.charging;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "charging_station")
public class ChargingStation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String stationId;
  private String status;
  private Instant lastSeenAt;

  protected ChargingStation() {}

  public ChargingStation(String stationId, String status) {
    this.stationId = stationId;
    this.status = status;
    this.lastSeenAt = Instant.now();
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

  public void heartbeat(String newStatus) {
    this.status = newStatus;
    this.lastSeenAt = Instant.now();
  }
}
