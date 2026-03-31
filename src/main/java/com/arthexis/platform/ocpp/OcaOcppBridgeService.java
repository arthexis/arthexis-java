package com.arthexis.platform.ocpp;

import com.arthexis.platform.charging.ChargingConnectorStateService;
import com.arthexis.platform.charging.ChargingStationAdminDetails;
import com.arthexis.platform.charging.ChargingStationService;
import com.arthexis.platform.telemetry.TelemetryIngestionService;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OcaOcppBridgeService {

  private final ChargingStationService chargingStationService;
  private final ChargingConnectorStateService connectorStateService;
  private final OcppSessionStateStore stateStore;
  private final TelemetryIngestionService telemetryIngestionService;
  private final OcaOcppPayloadNormalizer payloadNormalizer;

  public OcaOcppBridgeService(
      ChargingStationService chargingStationService,
      ChargingConnectorStateService connectorStateService,
      OcppSessionStateStore stateStore,
      TelemetryIngestionService telemetryIngestionService,
      OcaOcppPayloadNormalizer payloadNormalizer) {
    this.chargingStationService = chargingStationService;
    this.connectorStateService = connectorStateService;
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
        String connectorStatus =
            stringValue(
                payload.getOrDefault("status", payload.getOrDefault("connectorStatus", "UNKNOWN")));
        int evseId = resolveEvseId(payload);
        int connectorId = resolveConnectorId(payload);
        Instant reportedAt = resolveReportedAt(payload);
        connectorStateService.upsertConnectorState(
            stationId,
            evseId,
            connectorId,
            connectorStatus,
            optionalStringValue(payload.get("connectorType")),
            optionalStringValue(
                payload.getOrDefault("availability", payload.get("connectorAvailability"))),
            reportedAt);

        String aggregateStatus =
            connectorStateService.deriveStationAggregateStatus(stationId, connectorStatus);
        chargingStationService.upsertStatus(stationId, aggregateStatus, buildAdminDetails(payload, false));
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


  private int resolveEvseId(Map<String, Object> payload) {
    Map<String, Object> evse = mapValue(payload.get("evse"));
    Object evseId = evse.getOrDefault("id", payload.getOrDefault("evseId", 1));
    return intValue(evseId, 1);
  }

  private int resolveConnectorId(Map<String, Object> payload) {
    Map<String, Object> evse = mapValue(payload.get("evse"));
    Object connectorId =
        firstNonNull(
            evse.get("connectorId"),
            payload.get("connectorId"),
            payload.get("connector"),
            1);
    return intValue(connectorId, 1);
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

  private Instant resolveReportedAt(Map<String, Object> payload) {
    Object timestamp = payload.get("timestamp");
    if (timestamp == null) {
      return Instant.now();
    }
    if (timestamp instanceof Number number) {
      long numericTimestamp = number.longValue();
      long absoluteTimestamp = Math.abs(numericTimestamp);
      if (absoluteTimestamp <= 9_999_999_999L) {
        return Instant.ofEpochSecond(numericTimestamp);
      }
      if (absoluteTimestamp >= 1_000_000_000_000L) {
        return Instant.ofEpochMilli(numericTimestamp);
      }
      return Instant.now();
    }
    String value = timestamp.toString();
    try {
      return Instant.parse(value);
    } catch (RuntimeException ex) {
      return Instant.now();
    }
  }

  private String stringValue(Object value) {
    return value == null ? "" : value.toString();
  }

  private String optionalStringValue(Object value) {
    if (value == null) {
      return null;
    }
    String text = value.toString();
    return text.isBlank() ? null : text;
  }


  private Object firstNonNull(Object... values) {
    for (Object value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private int intValue(Object value, int defaultValue) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value == null) {
      return defaultValue;
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException ex) {
      return defaultValue;
    }
  }

  private Map<String, Object> accepted() {
    return Map.of("status", "Accepted");
  }
}
