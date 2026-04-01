package com.arthexis.platform.operations;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobExecutionTracker {

  private final JobExecutionRecordRepository repository;
  private final MeterRegistry meterRegistry;

  public JobExecutionTracker(JobExecutionRecordRepository repository, MeterRegistry meterRegistry) {
    this.repository = repository;
    this.meterRegistry = meterRegistry;
  }

  @Transactional
  public BeginResult begin(
      String jobId, String jobType, String stationScope, String idempotencyKey, int attempt) {
    Optional<JobExecutionRecord> existing = repository.findByIdempotencyKey(idempotencyKey);

    if (existing.isPresent()) {
      JobExecutionRecord record = existing.get();
      if (record.getStatus() == JobExecutionStatus.COMPLETED) {
        meterRegistry.counter("arthexis.jobs.consumed.duplicate", "job_type", jobType).increment();
        return BeginResult.duplicate(record);
      }
      record.markStarted(Math.max(record.getAttempt(), attempt));
      return BeginResult.started(repository.save(record));
    }

    JobExecutionRecord created =
        repository.save(new JobExecutionRecord(jobId, jobType, stationScope, idempotencyKey, attempt));
    return BeginResult.started(created);
  }

  @Transactional
  public void markCompleted(JobExecutionRecord record, String jobType) {
    record.markCompleted();
    repository.save(record);
    meterRegistry.counter("arthexis.jobs.consumed.success", "job_type", jobType).increment();
  }

  @Transactional
  public void markRetryScheduled(JobExecutionRecord record, String jobType, String error) {
    record.markRetryScheduled(error);
    repository.save(record);
    meterRegistry.counter("arthexis.jobs.consumed.retry", "job_type", jobType).increment();
  }

  @Transactional
  public void markFailed(JobExecutionRecord record, String jobType, String error) {
    record.markFailed(error);
    repository.save(record);
    meterRegistry.counter("arthexis.jobs.consumed.failed", "job_type", jobType).increment();
  }

  public record BeginResult(boolean duplicate, JobExecutionRecord record) {
    static BeginResult duplicate(JobExecutionRecord record) { return new BeginResult(true, record); }
    static BeginResult started(JobExecutionRecord record) { return new BeginResult(false, record); }
  }
}
