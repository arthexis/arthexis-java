package com.arthexis.platform.operations;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobExecutionRecordRepository extends JpaRepository<JobExecutionRecord, Long> {
  Optional<JobExecutionRecord> findByIdempotencyKey(String idempotencyKey);
}
