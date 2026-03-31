package com.arthexis.platform.ocpp;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcppSessionRecordRepository extends JpaRepository<OcppSessionRecord, Long> {
  Optional<OcppSessionRecord> findBySessionId(String sessionId);
}
