package com.arthexis.platform.ocpp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ocpp_message_record")
public class OcppMessageRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "session_record_id")
  private OcppSessionRecord sessionRecord;

  @Column(name = "session_id", nullable = false, length = 128)
  private String sessionId;

  @Column(name = "station_id", length = 64)
  private String stationId;

  @Column(name = "direction", nullable = false, length = 16)
  private String direction;

  @Column(name = "message_type", length = 32)
  private String messageType;

  @Column(name = "action", length = 64)
  private String action;

  @Column(name = "message_id", length = 128)
  private String messageId;

  @Column(name = "payload_snapshot", nullable = false, length = 8000)
  private String payloadSnapshot;

  @Column(name = "payload_truncated", nullable = false)
  private boolean payloadTruncated;

  @Column(name = "parse_status", nullable = false, length = 32)
  private String parseStatus;

  @Column(name = "result_status", length = 64)
  private String resultStatus;

  @Column(name = "sampled_at", nullable = false)
  private Instant sampledAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected OcppMessageRecord() {}

  public OcppMessageRecord(
      OcppSessionRecord sessionRecord,
      String sessionId,
      String stationId,
      String direction,
      String messageType,
      String action,
      String messageId,
      String payloadSnapshot,
      boolean payloadTruncated,
      String parseStatus,
      String resultStatus,
      Instant sampledAt,
      Instant createdAt) {
    this.sessionRecord = sessionRecord;
    this.sessionId = sessionId;
    this.stationId = stationId;
    this.direction = direction;
    this.messageType = messageType;
    this.action = action;
    this.messageId = messageId;
    this.payloadSnapshot = payloadSnapshot;
    this.payloadTruncated = payloadTruncated;
    this.parseStatus = parseStatus;
    this.resultStatus = resultStatus;
    this.sampledAt = sampledAt;
    this.createdAt = createdAt;
  }
}
