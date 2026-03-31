package com.arthexis.platform.ocpp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ocpp_command")
public class OcppCommandRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "command_id", nullable = false, unique = true, length = 64)
  private String commandId;

  @Column(name = "station_id", nullable = false, length = 64)
  private String stationId;

  @Column(name = "component", nullable = false, length = 64)
  private String component;

  @Column(name = "action", nullable = false, length = 64)
  private String action;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private OcppCommandStatus status;

  @Column(name = "payload_json", nullable = false, length = 20000)
  private String payloadJson;

  @Column(name = "message_id", length = 128)
  private String messageId;

  @Column(name = "attempt_count", nullable = false)
  private int attemptCount;

  @Column(name = "max_attempts", nullable = false)
  private int maxAttempts;

  @Column(name = "next_attempt_at")
  private Instant nextAttemptAt;

  @Column(name = "requested_by", length = 128)
  private String requestedBy;

  @Column(name = "failure_reason", length = 512)
  private String failureReason;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Column(name = "queued_at")
  private Instant queuedAt;

  @Column(name = "sent_at")
  private Instant sentAt;

  @Column(name = "acknowledged_at")
  private Instant acknowledgedAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected OcppCommandRecord() {}

  public OcppCommandRecord(
      String stationId,
      String component,
      String action,
      String payloadJson,
      String requestedBy,
      int maxAttempts) {
    Instant now = Instant.now();
    this.commandId = UUID.randomUUID().toString();
    this.stationId = stationId;
    this.component = component;
    this.action = action;
    this.payloadJson = payloadJson;
    this.requestedBy = requestedBy;
    this.maxAttempts = maxAttempts;
    this.status = OcppCommandStatus.REQUESTED;
    this.requestedAt = now;
    this.updatedAt = now;
  }

  public Long getId() {
    return id;
  }

  public String getCommandId() {
    return commandId;
  }

  public String getStationId() {
    return stationId;
  }

  public String getComponent() {
    return component;
  }

  public String getAction() {
    return action;
  }

  public OcppCommandStatus getStatus() {
    return status;
  }

  public String getPayloadJson() {
    return payloadJson;
  }

  public String getMessageId() {
    return messageId;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public Instant getQueuedAt() {
    return queuedAt;
  }

  public Instant getSentAt() {
    return sentAt;
  }

  public Instant getAcknowledgedAt() {
    return acknowledgedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void markQueued(Instant now, Instant nextAttemptAt) {
    this.status = OcppCommandStatus.QUEUED;
    this.queuedAt = this.queuedAt == null ? now : this.queuedAt;
    this.nextAttemptAt = nextAttemptAt;
    this.updatedAt = now;
  }

  public void markSent(String messageId, Instant now, Instant timeoutAt) {
    this.status = OcppCommandStatus.SENT;
    this.messageId = messageId;
    this.sentAt = now;
    this.attemptCount += 1;
    this.nextAttemptAt = timeoutAt;
    this.failureReason = null;
    this.updatedAt = now;
  }

  public void markAcknowledged(Instant now) {
    this.status = OcppCommandStatus.ACKNOWLEDGED;
    this.acknowledgedAt = now;
    this.nextAttemptAt = null;
    this.updatedAt = now;
  }

  public void markTimedOut(Instant now, String reason) {
    this.status = OcppCommandStatus.TIMED_OUT;
    this.failureReason = reason;
    this.nextAttemptAt = null;
    this.updatedAt = now;
  }

  public void markFailed(Instant now, String reason) {
    this.status = OcppCommandStatus.FAILED;
    this.failureReason = reason;
    this.nextAttemptAt = null;
    this.updatedAt = now;
  }
}
