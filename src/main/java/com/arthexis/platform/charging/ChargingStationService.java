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
    ChargingStation station =
        repository.findByStationId(stationId).orElseGet(() -> new ChargingStation(stationId, status));
    station.heartbeat(status);
    return repository.save(station);
  }
}
