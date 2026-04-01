package com.arthexis.platform.app.readapi;

import com.arthexis.platform.firmware.FirmwareRolloutStatus;
import com.arthexis.platform.firmware.FirmwareRolloutStatusRepository;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/read/firmware")
@ConditionalOnProperty(
    prefix = "arthexis.features.read-api",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false)
public class FirmwareReadController {

  private final FirmwareRolloutStatusRepository repository;

  public FirmwareReadController(FirmwareRolloutStatusRepository repository) {
    this.repository = repository;
  }

  @GetMapping("/rollouts")
  public Page<FirmwareRolloutStatusResponse> rollouts(
      @RequestParam String tenantId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return repository
        .findByTenantId(tenantId, PageRequest.of(page, size, Sort.by("updatedAt").descending()))
        .map(FirmwareRolloutStatusResponse::from);
  }

  public record FirmwareRolloutStatusResponse(
      String stationId,
      String tenantId,
      String campaignId,
      String targetVersion,
      String rolloutState,
      Instant updatedAt) {

    static FirmwareRolloutStatusResponse from(FirmwareRolloutStatus rolloutStatus) {
      return new FirmwareRolloutStatusResponse(
          rolloutStatus.getStationId(),
          rolloutStatus.getTenantId(),
          rolloutStatus.getCampaignId(),
          rolloutStatus.getTargetVersion(),
          rolloutStatus.getRolloutState(),
          rolloutStatus.getUpdatedAt());
    }
  }
}
