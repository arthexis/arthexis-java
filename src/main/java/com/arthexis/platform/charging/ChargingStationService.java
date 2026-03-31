package com.arthexis.platform.charging;

import com.arthexis.platform.app.admin.StationStatusChangedEvent;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargingStationService {

  private final ChargingStationRepository repository;
  private final ApplicationEventPublisher eventPublisher;

  public ChargingStationService(
      ChargingStationRepository repository, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public ChargingStation upsertStatus(String stationId, String status) {
    return upsertStatus(stationId, status, null);
  }

  @Transactional
  public ChargingStation upsertStatus(
      String stationId, String status, ChargingStationAdminDetails adminDetails) {
    ChargingStation station =
        repository
            .findByStationId(stationId)
            .orElseGet(() -> new ChargingStation(stationId, defaultStatus(status)));

    String previousStatus = station.getStatus();
    String resolvedStatus = defaultStatus(status);
    station.heartbeat(resolvedStatus);
    station.applyAdminDetails(adminDetails);

    ChargingStation persisted = repository.save(station);

    if (!resolvedStatus.equals(previousStatus)) {
      eventPublisher.publishEvent(
          new StationStatusChangedEvent(
              persisted.getStationId(),
              persisted.getStatus(),
              previousStatus,
              persisted.getTenantId(),
              persisted.getSiteId(),
              Instant.now()));
    }

    return persisted;
  }

  private String defaultStatus(String status) {
    return status == null || status.isBlank() ? "ONLINE" : status;
  }
}
