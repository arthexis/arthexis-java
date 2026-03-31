package com.arthexis.platform.ocpp;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcppCommandRecordRepository extends JpaRepository<OcppCommandRecord, Long> {
  List<OcppCommandRecord> findByStatusInAndNextAttemptAtLessThanEqual(
      List<OcppCommandStatus> statuses, Instant threshold);

  Optional<OcppCommandRecord> findByMessageIdAndStatus(String messageId, OcppCommandStatus status);

  Page<OcppCommandRecord> findByStationId(String stationId, Pageable pageable);
}

