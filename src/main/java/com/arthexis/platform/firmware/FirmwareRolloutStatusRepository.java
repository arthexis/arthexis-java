package com.arthexis.platform.firmware;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FirmwareRolloutStatusRepository extends JpaRepository<FirmwareRolloutStatus, Long> {

  Optional<FirmwareRolloutStatus> findByCampaignIdAndStationId(String campaignId, String stationId);

  List<FirmwareRolloutStatus> findByTenantIdOrderByUpdatedAtDesc(String tenantId);

  Page<FirmwareRolloutStatus> findByTenantId(String tenantId, Pageable pageable);
}
