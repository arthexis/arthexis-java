package com.arthexis.platform.ocpp;

import com.arthexis.platform.charging.ChargingStationService;
import com.arthexis.platform.telemetry.TelemetryIngestionService;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OcaOcppBridgeService {

  private final ChargingStationService chargingStationService;
  private final OcppSessionStateStore stateStore;
  private final TelemetryIngestionService telemetryIngestionService;
  private final OcaOcppPayloadNormalizer payloadNormalizer;

  public OcaOcppBridgeService(
      ChargingStationService chargingStationService,
      OcppSessionStateStore stateStore,
      TelemetryIngestionService telemetryIngestionService,
      OcaOcppPayloadNormalizer payloadNormalizer) {
    this.chargingStationService = chargingStationService;
    this.stateStore = stateStore;
    this.telemetryIngestionService = telemetryIngestionService;
    this.payloadNormalizer = payloadNormalizer;
  }

  public Map<String, Object> handleIncoming(String sessionId, OcppMessage incoming) {
    if (!(incoming.payload() instanceof Map<?, ?> rawPayload)) {
      return accepted();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) rawPayload;
    String stationId = payloadNormalizer.resolveStationId(sessionId, payload);

    switch (incoming.action()) {
      case "Heartbeat" -> {
        chargingStationService.upsertStatus(stationId, "ONLINE");
        stateStore.storePendingCommand(stationId, incoming.messageId(), incoming.action());
        return Map.of("currentTime", Instant.now().toString());
      }
      case "BootNotification" -> {
        chargingStationService.upsertStatus(stationId, "ONLINE");
        stateStore.storePendingCommand(stationId, incoming.messageId(), incoming.action());
        return Map.of(
            "status", "Accepted",
            "currentTime", Instant.now().toString(),
            "interval", 300);
      }
      case "StatusNotification" -> {
        String status =
            stringValue(payload.getOrDefault("status", payload.getOrDefault("connectorStatus", "ONLINE")));
        chargingStationService.upsertStatus(stationId, status);
        return accepted();
      }
      case "MeterValues" -> {
        telemetryIngestionService.ingestMeterValues(
            stationId, payloadNormalizer.normalizeMeterValues(stationId, payload));
        return accepted();
      }
      case "TransactionEvent" -> {
        telemetryIngestionService.ingestMeterValues(
            stationId, payloadNormalizer.normalizeTransactionEvent(stationId, payload));
        Object eventType = payload.get("eventType");
        if ("Started".equals(eventType)) {
          chargingStationService.upsertStatus(stationId, "CHARGING");
        }
        if ("Ended".equals(eventType)) {
          chargingStationService.upsertStatus(stationId, "AVAILABLE");
        }
        return accepted();
      }
      default -> {
        return accepted();
      }
    }
  }

  private String stringValue(Object value) {
    return value == null ? "" : value.toString();
  }

  private Map<String, Object> accepted() {
    return Map.of("status", "Accepted");
  }
}
