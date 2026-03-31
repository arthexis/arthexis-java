package com.arthexis.platform.charging;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChargingStationService {

  private final ChargingStationRepository repository;

  public ChargingStationService(ChargingStationRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public ChargingStation upsertStatus(String stationId, String status) {
    String normalizedStatus = normalizeStatus(status);
    ChargingStation station =
        repository
            .findByStationId(stationId)
            .orElseGet(() -> new ChargingStation(stationId, normalizedStatus));
    station.heartbeat(normalizedStatus);
    return repository.save(station);
  }

  @Transactional
  public ChargingStation markOffline(String stationId) {
    return upsertStatus(stationId, "OFFLINE");
  }

  @Transactional(readOnly = true)
  public List<ChargingStation> listStations() {
    return repository.findAll();
  }

  @Transactional(readOnly = true)
  public ChargingStation getStation(String stationId) {
    return repository
        .findByStationId(stationId)
        .orElseThrow(() -> new ChargingStationNotFoundException(stationId));
  }

  private String normalizeStatus(String status) {
    if (!StringUtils.hasText(status)) {
      throw new IllegalArgumentException("status must be provided");
    }
    return status.trim().toUpperCase();
  }
}
