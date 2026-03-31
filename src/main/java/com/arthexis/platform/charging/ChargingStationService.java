package com.arthexis.platform.charging;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargingStationService {

  private final ChargingStationRepository repository;

  public ChargingStationService(ChargingStationRepository repository) {
    this.repository = repository;
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

    station.heartbeat(defaultStatus(status));
    station.applyAdminDetails(adminDetails);

    return repository.save(station);
  }

  private String defaultStatus(String status) {
    return status == null || status.isBlank() ? "ONLINE" : status;
  }
}
