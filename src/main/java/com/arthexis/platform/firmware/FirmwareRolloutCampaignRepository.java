package com.arthexis.platform.firmware;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FirmwareRolloutCampaignRepository extends JpaRepository<FirmwareRolloutCampaign, Long> {

  boolean existsByCampaignId(String campaignId);
}
