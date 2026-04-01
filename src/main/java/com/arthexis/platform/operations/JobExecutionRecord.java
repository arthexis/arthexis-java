package com.arthexis.platform.operations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "job_execution")
public class JobExecutionRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "job_id", nullable = false, length = 64)
  private String jobId;

  @Column(name = "job_type", nullable = false, length = 64)
  private String jobType;

  @Column(name = "station_scope", length = 128)
  private String stationScope;

  @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
  private String idempotencyKey;

  @Column(name = "attempt", nullable = false)
  private int attempt;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private JobExecutionStatus status;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "finished_at")
  private Instant finishedAt;

  @Column(name = "error_message", length = 512)
  private String errorMessage;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected JobExecutionRecord() {}

  public JobExecutionRecord(
      String jobId, String jobType, String stationScope, String idempotencyKey, int attempt) {
    this.jobId = jobId;
    this.jobType = jobType;
    this.stationScope = stationScope;
    this.idempotencyKey = idempotencyKey;
    this.attempt = attempt;
    this.status = JobExecutionStatus.STARTED;
    this.startedAt = Instant.now();
  }

  public Long getId() { return id; }
  public String getJobId() { return jobId; }
  public String getJobType() { return jobType; }
  public String getStationScope() { return stationScope; }
  public String getIdempotencyKey() { return idempotencyKey; }
  public int getAttempt() { return attempt; }
  public JobExecutionStatus getStatus() { return status; }
  public Instant getStartedAt() { return startedAt; }
  public Instant getFinishedAt() { return finishedAt; }
  public String getErrorMessage() { return errorMessage; }

  public void markStarted(int nextAttempt) {
    this.attempt = nextAttempt;
    this.status = JobExecutionStatus.STARTED;
    this.startedAt = Instant.now();
    this.finishedAt = null;
    this.errorMessage = null;
  }

  public void markCompleted() {
    this.status = JobExecutionStatus.COMPLETED;
    this.finishedAt = Instant.now();
    this.errorMessage = null;
  }

  public void markRetryScheduled(String error) {
    this.status = JobExecutionStatus.RETRY_SCHEDULED;
    this.finishedAt = Instant.now();
    this.errorMessage = truncate(error);
  }

  public void markFailed(String error) {
    this.status = JobExecutionStatus.FAILED;
    this.finishedAt = Instant.now();
    this.errorMessage = truncate(error);
  }

  private String truncate(String value) {
    if (value == null) return null;
    return value.length() <= 512 ? value : value.substring(0, 512);
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
