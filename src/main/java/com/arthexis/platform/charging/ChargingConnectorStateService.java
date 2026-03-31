package com.arthexis.platform.charging;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChargingConnectorStateService {

  private static final List<String> STATION_STATUS_PRECEDENCE =
      List.of(
          "FAULTED",
          "UNAVAILABLE",
          "CHARGING",
          "SUSPENDED_EVSE",
          "SUSPENDED_EV",
          "PREPARING",
          "FINISHING",
          "RESERVED",
          "OCCUPIED",
          "AVAILABLE",
          "ONLINE",
          "UNKNOWN");

  private static final Map<String, String> OCPP_STATUS_TO_STATION_STATUS =
      Map.ofEntries(
          Map.entry("FAULTED", "FAULTED"),
          Map.entry("UNAVAILABLE", "UNAVAILABLE"),
          Map.entry("CHARGING", "CHARGING"),
          Map.entry("SUSPENDED_EVSE", "SUSPENDED_EVSE"),
          Map.entry("SUSPENDED_EV", "SUSPENDED_EV"),
          Map.entry("PREPARING", "PREPARING"),
          Map.entry("FINISHING", "FINISHING"),
          Map.entry("RESERVED", "RESERVED"),
          Map.entry("OCCUPIED", "OCCUPIED"),
          Map.entry("AVAILABLE", "AVAILABLE"),
          Map.entry("UNKNOWN", "UNKNOWN"));

  private final ChargingConnectorStateRepository repository;

  public ChargingConnectorStateService(ChargingConnectorStateRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public ChargingConnectorState upsertConnectorState(
      String stationId,
      int evseId,
      int connectorId,
      String connectorStatus,
      String connectorType,
      String availability,
      Instant reportedAt) {
    String normalizedStatus = normalizeStatus(connectorStatus);
    String normalizedAvailability = normalizeOptional(availability);
    String normalizedConnectorType = normalizeOptional(connectorType);
    Instant timestamp = reportedAt == null ? Instant.now() : reportedAt;

    ChargingConnectorState connectorState =
        repository
            .findByStationIdAndEvseIdAndConnectorId(stationId, evseId, connectorId)
            .orElseGet(
                () ->
                    new ChargingConnectorState(
                        stationId,
                        evseId,
                        connectorId,
                        normalizedStatus,
                        normalizedConnectorType,
                        normalizedAvailability,
                        timestamp));

    String mergedConnectorType =
        normalizedConnectorType != null ? normalizedConnectorType : connectorState.getConnectorType();
    String mergedAvailability =
        normalizedAvailability != null ? normalizedAvailability : connectorState.getAvailability();

    connectorState.updateState(
        normalizedStatus, mergedConnectorType, mergedAvailability, timestamp);
    return repository.save(connectorState);
  }

  @Transactional(readOnly = true)
  public String deriveStationAggregateStatus(String stationId, String fallbackStatus) {
    List<ChargingConnectorState> connectors = repository.findByStationIdOrderByEvseIdAscConnectorIdAsc(stationId);
    if (connectors.isEmpty()) {
      return normalizeFallbackStatus(fallbackStatus);
    }

    String winningStatus = "UNKNOWN";
    int winningRank = STATION_STATUS_PRECEDENCE.size();

    for (ChargingConnectorState connector : connectors) {
      String candidate = toStationStatus(connector.getConnectorStatus());
      int rank = rank(candidate);
      if (rank < winningRank) {
        winningStatus = candidate;
        winningRank = rank;
      }
    }

    return winningStatus;
  }

  private String normalizeStatus(String status) {
    String normalized = normalizeOptional(status);
    return normalized == null ? "UNKNOWN" : normalized;
  }

  private String normalizeFallbackStatus(String fallbackStatus) {
    String normalized = normalizeOptional(fallbackStatus);
    return normalized == null ? "ONLINE" : normalized;
  }

  private String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String trimmed = value.trim().replace('-', '_').replace(' ', '_');
    String snakeLike = trimmed.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
    return snakeLike.toUpperCase();
  }

  private String toStationStatus(String connectorStatus) {
    return OCPP_STATUS_TO_STATION_STATUS.getOrDefault(connectorStatus, connectorStatus);
  }

  private int rank(String status) {
    int index = STATION_STATUS_PRECEDENCE.indexOf(status);
    return index >= 0 ? index : STATION_STATUS_PRECEDENCE.size() - 1;
  }
}
