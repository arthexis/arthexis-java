package com.arthexis.platform.firmware;

import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FirmwareService {

  private final FirmwareRolloutStatusRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  public FirmwareService(
      FirmwareRolloutStatusRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public FirmwareRolloutStatus upsertRolloutStatus(
      String campaignId,
      String stationId,
      String tenantId,
      String targetVersion,
      String rolloutState) {
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
            Instant.now()));

    return persisted;
  }
}
