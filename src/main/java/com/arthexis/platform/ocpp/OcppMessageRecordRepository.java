package com.arthexis.platform.ocpp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcppMessageRecordRepository extends JpaRepository<OcppMessageRecord, Long> {

  Page<OcppMessageRecord> findByStationId(String stationId, Pageable pageable);
}
