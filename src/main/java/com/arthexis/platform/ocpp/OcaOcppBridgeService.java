package com.arthexis.platform.ocpp;

import com.arthexis.platform.auth.AuthorizationDecision;
import com.arthexis.platform.auth.RfidAuthorizationGateway;
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
  private final RfidAuthorizationGateway rfidAuthorizationService;


  public OcaOcppBridgeService(
      ChargingStationService chargingStationService,
      ChargingConnectorStateService connectorStateService,
      OcppSessionStateStore stateStore,
      TelemetryIngestionService telemetryIngestionService,
      OcaOcppPayloadNormalizer payloadNormalizer,
      RfidAuthorizationGateway rfidAuthorizationService) {
    this.chargingStationService = chargingStationService;
    this.connectorStateService = connectorStateService;
    this.stateStore = stateStore;
    this.telemetryIngestionService = telemetryIngestionService;
    this.payloadNormalizer = payloadNormalizer;
    this.rfidAuthorizationService = rfidAuthorizationService;
  }

  public OcppBridgeResponse handleIncoming(String sessionId, OcppMessage incoming) {
    if (!(incoming.payload() instanceof Map<?, ?> rawPayload)) {
      return response(null, accepted());
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> payload = (Map<String, Object>) rawPayload;
    String stationId = payloadNormalizer.resolveStationId(sessionId, payload);

    return switch (incoming.action()) {
      case "Heartbeat" -> {
        chargingStationService.upsertStatus(stationId, "ONLINE", buildAdminDetails(payload, false));
        stateStore.storePendingCommand(stationId, incoming.messageId(), incoming.action());
        yield response(stationId, Map.of("currentTime", Instant.now().toString()));
      }
      case "BootNotification" -> {
        chargingStationService.upsertStatus(stationId, "ONLINE", buildAdminDetails(payload, true));
        stateStore.storePendingCommand(stationId, incoming.messageId(), incoming.action());
        yield response(
            stationId,
            Map.of("status", "Accepted", "currentTime", Instant.now().toString(), "interval", 300));
      }
      case "StatusNotification" -> {
        String connectorStatus =
            stringValue(payload.getOrDefault("status", payload.getOrDefault("connectorStatus", "UNKNOWN")));
        int evseId = resolveEvseId(payload);
        int connectorId = resolveConnectorId(payload);
        connectorStateService.upsertConnectorState(
            stationId,
            evseId,
            connectorId,
            connectorStatus,
            stringValue(payload.get("connectorType")),
            stringValue(payload.getOrDefault("availability", payload.get("connectorAvailability"))),
            Instant.now());

        String aggregateStatus =
            connectorStateService.deriveStationAggregateStatus(stationId, connectorStatus);
        chargingStationService.upsertStatus(stationId, aggregateStatus, buildAdminDetails(payload, false));
        yield response(stationId, accepted());
      }
      case "MeterValues" -> {
        telemetryIngestionService.ingestMeterValues(
            stationId, payloadNormalizer.normalizeMeterValues(stationId, payload));
        yield response(stationId, accepted());
      }
      case "Authorize" -> {
        String cardUid = resolveCardUid(payload);
        AuthorizationDecision decision = rfidAuthorizationService.authorize(stationId, cardUid);
        yield response(stationId, authorizePayload(decision, payload));
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
        yield response(stationId, accepted());
      }
      case "DiagnosticsStatusNotification" -> {
        chargingStationService.upsertStatus(stationId, "ONLINE", buildAdminDetails(payload, false));
        yield response(stationId, accepted());
      }
      case "FirmwareStatusNotification" -> {
        chargingStationService.upsertStatus(stationId, "ONLINE", buildAdminDetails(payload, false));
        yield response(stationId, accepted());
      }
      case "AvailabilityStatusNotification" -> {
        Map<String, Object> normalized = payloadNormalizer.normalizeAvailabilityStatus(stationId, payload);
        int evseId = intValue(normalized.get("evseId"), 1);
        int connectorId = intValue(normalized.get("connectorId"), 1);
        String availabilityStatus = stringValue(normalized.getOrDefault("status", "Operative"));
        connectorStateService.upsertConnectorState(
            stationId,
            evseId,
            connectorId,
            availabilityStatus,
            "",
            availabilityStatus,
            resolveEventTimestamp(normalized));
        yield response(stationId, accepted());
      }
      default -> response(stationId, accepted());
    };
  }

  private OcppBridgeResponse response(String stationId, Map<String, Object> payload) {
    return new OcppBridgeResponse(stationId, payload, stringValue(payload.get("status")));
  }

  private int resolveEvseId(Map<String, Object> payload) {
    Map<String, Object> evse = mapValue(payload.get("evse"));
    Object evseId = evse.getOrDefault("id", payload.getOrDefault("evseId", 1));
    return intValue(evseId, 1);
  }

  private int resolveConnectorId(Map<String, Object> payload) {
    Map<String, Object> evse = mapValue(payload.get("evse"));
    Object connectorId =
        firstNonNull(evse.get("connectorId"), payload.get("connectorId"), payload.get("connector"), 1);
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
            stringValue(payload.get("chargePointModel")), stringValue(chargingStation.get("model")));

    String protocolVersion =
        firstNonBlank(
            stringValue(payload.get("ocppVersion")), stringValue(payload.get("protocolVersion")));

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

  private Map<String, Object> authorizePayload(AuthorizationDecision decision, Map<String, Object> payload) {
    String cardUid = resolveCardUid(payload);
    if (payload.containsKey("idToken")) {
      Map<String, Object> idTokenInfo =
          Map.of(
              "status", decision.ocppStatus(),
              "customData", Map.of("authMode", decision.authMode(), "reason", decision.reason()));

      return decision.loginUrl() == null
          ? Map.of(
              "idTokenInfo",
              idTokenInfo,
              "customData",
              Map.of(
                  "accountExternalId", nullable(decision.accountExternalId()),
                  "cardUid", nullable(cardUid)))
          : Map.of(
              "idTokenInfo",
              idTokenInfo,
              "customData",
                  Map.of(
                      "accountExternalId", nullable(decision.accountExternalId()),
                      "cardUid", nullable(cardUid),
                      "loginUrl", decision.loginUrl()));
    }

    return Map.of("idTagInfo", Map.of("status", decision.ocppStatus()));
  }

  private String nullable(String value) {
    return value == null ? "" : value;
  }

  private String resolveCardUid(Map<String, Object> payload) {
    String idTag = stringValue(payload.get("idTag"));
    if (!idTag.isBlank()) {
      return idTag;
    }
    Map<String, Object> idToken = mapValue(payload.get("idToken"));
    return firstNonBlank(stringValue(idToken.get("idToken")), stringValue(payload.get("token")), "");
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

  private Instant resolveEventTimestamp(Map<String, Object> normalized) {
    String timestamp = stringValue(normalized.get("timestamp"));
    if (timestamp.isBlank()) {
      return Instant.now();
    }
    try {
      return Instant.parse(timestamp);
    } catch (Exception ex) {
      return Instant.now();
    }
  }

  private Map<String, Object> accepted() {
    return Map.of("status", "Accepted");
  }
}
