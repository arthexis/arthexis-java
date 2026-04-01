package com.arthexis.platform.firmware;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FirmwareService {

  private final FirmwareRolloutStatusRepository repository;
  private final FirmwareRolloutCampaignRepository campaignRepository;
  private final ApplicationEventPublisher eventPublisher;

  public FirmwareService(
      FirmwareRolloutStatusRepository repository,
      FirmwareRolloutCampaignRepository campaignRepository,
      ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.campaignRepository = campaignRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public FirmwareRolloutStatus upsertRolloutStatus(
      String campaignId,
      String stationId,
      String tenantId,
      String targetVersion,
      String rolloutState) {
    ensureCampaignExists(campaignId, tenantId, targetVersion, rolloutState);

    FirmwareRolloutStatus status =
        repository
            .findByCampaignIdAndStationId(campaignId, stationId)
            .orElseGet(
                () ->
                    new FirmwareRolloutStatus(
                        stationId, tenantId, campaignId, targetVersion, rolloutState));

    status.apply(targetVersion, rolloutState);
    FirmwareRolloutStatus persisted = repository.save(status);

    eventPublisher.publishEvent(
        new FirmwareRolloutStatusChangedEvent(
            persisted.getCampaignId(),
            persisted.getStationId(),
            persisted.getTenantId(),
            persisted.getTargetVersion(),
            persisted.getRolloutState(),
            persisted.getUpdatedAt()));

    return persisted;
  }

  private void ensureCampaignExists(
      String campaignId, String tenantId, String targetVersion, String rolloutState) {
    if (!campaignRepository.existsByCampaignId(campaignId)) {
      campaignRepository.save(
          new FirmwareRolloutCampaign(campaignId, tenantId, targetVersion, rolloutState));
    }
  }
}
