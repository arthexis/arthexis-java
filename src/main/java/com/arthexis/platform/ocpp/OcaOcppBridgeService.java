package com.arthexis.platform.ocpp;

import com.arthexis.platform.charging.ChargingStationAdminDetails;
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
        chargingStationService.upsertStatus(
            stationId, "ONLINE", buildAdminDetails(payload, false));
        stateStore.storePendingCommand(stationId, incoming.messageId(), incoming.action());
        return Map.of("currentTime", Instant.now().toString());
      }
      case "BootNotification" -> {
        chargingStationService.upsertStatus(
            stationId, "ONLINE", buildAdminDetails(payload, true));
        stateStore.storePendingCommand(stationId, incoming.messageId(), incoming.action());
        return Map.of(
            "status", "Accepted",
            "currentTime", Instant.now().toString(),
            "interval", 300);
      }
      case "StatusNotification" -> {
        String status =
            stringValue(payload.getOrDefault("status", payload.getOrDefault("connectorStatus", "ONLINE")));
        chargingStationService.upsertStatus(stationId, status, buildAdminDetails(payload, false));
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



  private ChargingStationAdminDetails buildAdminDetails(
      Map<String, Object> payload, boolean includeBootTime) {
    Map<String, Object> chargingStation = mapValue(payload.get("chargingStation"));

    String displayName =
        firstNonBlank(
            stringValue(payload.get("displayName")),
            stringValue(payload.get("stationName")),
            stringValue(chargingStation.get("displayName")));

    String vendor =
        firstNonBlank(
            stringValue(payload.get("chargePointVendor")),
            stringValue(chargingStation.get("vendorName")),
            stringValue(chargingStation.get("vendor")));

    String model =
        firstNonBlank(
            stringValue(payload.get("chargePointModel")),
            stringValue(chargingStation.get("model")));

    String protocolVersion =
        firstNonBlank(stringValue(payload.get("ocppVersion")), stringValue(payload.get("protocolVersion")));

    String firmwareVersion =
        firstNonBlank(
            stringValue(payload.get("firmwareVersion")),
            stringValue(chargingStation.get("firmwareVersion")));

    String tenantId =
        firstNonBlank(
            stringValue(payload.get("tenantId")),
            stringValue(payload.get("organizationId")),
            stringValue(payload.get("customerId")));

    String siteId =
        firstNonBlank(
            stringValue(payload.get("siteId")),
            stringValue(payload.get("locationId")),
            stringValue(payload.get("chargingStationId")));

    return new ChargingStationAdminDetails(
        nullIfBlank(displayName),
        nullIfBlank(vendor),
        nullIfBlank(model),
        nullIfBlank(protocolVersion),
        nullIfBlank(firmwareVersion),
        nullIfBlank(tenantId),
        nullIfBlank(siteId),
        null,
        Instant.now(),
        includeBootTime ? Instant.now() : null);
  }

  private Map<String, Object> mapValue(Object value) {
    if (value instanceof Map<?, ?> rawMap) {
      @SuppressWarnings("unchecked")
      Map<String, Object> casted = (Map<String, Object>) rawMap;
      return casted;
    }
    return Map.of();
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String nullIfBlank(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
  private String stringValue(Object value) {
    return value == null ? "" : value.toString();
  }

  private Map<String, Object> accepted() {
    return Map.of("status", "Accepted");
  }
}
