package com.arthexis.platform.ocpp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ocpp_session")
public class OcppSessionRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id", nullable = false, unique = true, length = 128)
  private String sessionId;

  @Column(name = "station_id", length = 64)
  private String stationId;

  @Column(name = "connected_at", nullable = false)
  private Instant connectedAt;

  @Column(name = "disconnected_at")
  private Instant disconnectedAt;

  @Column(name = "last_message_at")
  private Instant lastMessageAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected OcppSessionRecord() {}

  public OcppSessionRecord(String sessionId, Instant connectedAt) {
    this.sessionId = sessionId;
    this.connectedAt = connectedAt;
    this.createdAt = connectedAt;
    this.updatedAt = connectedAt;
  }

  public Long getId() {
    return id;
  }

  public String getSessionId() {
    return sessionId;
  }

  public String getStationId() {
    return stationId;
  }

  public void setStationId(String stationId) {
    this.stationId = stationId;
  }

  public Instant getConnectedAt() {
    return connectedAt;
  }

  public Instant getDisconnectedAt() {
    return disconnectedAt;
  }

  public void setDisconnectedAt(Instant disconnectedAt) {
    this.disconnectedAt = disconnectedAt;
  }

  public Instant getLastMessageAt() {
    return lastMessageAt;
  }

  public void setLastMessageAt(Instant lastMessageAt) {
    this.lastMessageAt = lastMessageAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
