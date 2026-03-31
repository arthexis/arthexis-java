package com.arthexis.platform.ocpp;

import com.arthexis.platform.telemetry.TelemetryIngestionService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OcaOcppPayloadNormalizer {

  public String resolveStationId(String sessionId, Map<String, Object> payload) {
    Object explicitStationId = payload.get("stationId");
    if (explicitStationId instanceof String text && !text.isBlank()) {
      return text;
    }

    Object chargingStation = payload.get("chargingStation");
    if (chargingStation instanceof Map<?, ?> rawChargingStation) {
      Object serial = rawChargingStation.get("serialNumber");
      if (serial instanceof String serialText && !serialText.isBlank()) {
        return serialText;
      }
      Object model = rawChargingStation.get("model");
      if (model instanceof String modelText && !modelText.isBlank()) {
        return modelText;
      }
    }

    return sessionId;
  }

  public Map<String, Object> normalizeMeterValues(String stationId, Map<String, Object> payload) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    normalized.put("stationId", stationId);

    NormalizedSampleExtraction extraction = extractSampledValues(payload);
    normalized.putAll(extraction.flattenedValues());
    normalized.put(TelemetryIngestionService.STRUCTURED_SAMPLES_KEY, extraction.structuredSamples());

    if (!normalized.containsKey("sampledAt")) {
      normalized.put("sampledAt", Instant.now().toString());
    }
    return normalized;
  }

  public Map<String, Object> normalizeTransactionEvent(String stationId, Map<String, Object> payload) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    normalized.put("stationId", stationId);

    Object timestamp = payload.get("timestamp");
    if (timestamp instanceof String text) {
      normalized.put("sampledAt", text);
    }

    Object totalCost = payload.get("totalCost");
    if (totalCost instanceof Number number) {
      normalized.put("totalCost", number);
    }

    NormalizedSampleExtraction extraction = extractSampledValues(payload);
    normalized.putAll(extraction.flattenedValues());
    normalized.put(TelemetryIngestionService.STRUCTURED_SAMPLES_KEY, extraction.structuredSamples());

    if (!normalized.containsKey("sampledAt")) {
      normalized.put("sampledAt", Instant.now().toString());
    }
    return normalized;
  }

  public Map<String, Object> normalizeAuthorize(String stationId, Map<String, Object> payload) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    normalized.put("stationId", stationId);
    copyIfPresent(normalized, payload, "idToken");
    copyIfPresent(normalized, payload, "idTag");
    copyIfPresent(normalized, payload, "certificateStatus");
    return normalized;
  }

  public Map<String, Object> normalizeDiagnosticsStatus(
      String stationId, Map<String, Object> payload) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    normalized.put("stationId", stationId);
    String status =
        firstNonBlank(
            stringValue(payload.get("status")),
            stringValue(payload.get("uploadStatus")),
            stringValue(payload.get("diagnosticsStatus")));
    if (!status.isBlank()) {
      normalized.put("status", status);
    }
    return normalized;
  }

  public Map<String, Object> normalizeFirmwareStatus(String stationId, Map<String, Object> payload) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    normalized.put("stationId", stationId);
    String status =
        firstNonBlank(
            stringValue(payload.get("status")),
            stringValue(payload.get("firmwareStatus")),
            stringValue(payload.get("updateStatus")));
    if (!status.isBlank()) {
      normalized.put("status", status);
    }
    copyIfPresent(normalized, payload, "requestId");
    return normalized;
  }

  public Map<String, Object> normalizeAvailabilityStatus(
      String stationId, Map<String, Object> payload) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    normalized.put("stationId", stationId);
    String status =
        firstNonBlank(
            stringValue(payload.get("status")),
            stringValue(payload.get("operationalStatus")),
            stringValue(payload.get("availabilityType")));
    if (!status.isBlank()) {
      normalized.put("status", status);
    }
    String evseId = resolveEvseId(payload);
    String connectorId = resolveConnectorId(payload);
    if (!evseId.isBlank()) {
      normalized.put("evseId", evseId);
    }
    if (!connectorId.isBlank()) {
      normalized.put("connectorId", connectorId);
    }
    return normalized;
  }

  private NormalizedSampleExtraction extractSampledValues(Map<String, Object> payload) {
    Map<String, Object> flattened = new LinkedHashMap<>();
    List<Map<String, Object>> structuredSamples = new ArrayList<>();
    List<Map<String, Object>> meterValues = asObjectMapList(payload.get("meterValue"));
    if (meterValues.isEmpty()) {
      meterValues = asObjectMapList(payload.get("meterValues"));
    }

    String payloadTransactionId = resolveTransactionId(payload);
    String payloadEvseId = resolveEvseId(payload);
    String payloadConnectorId = resolveConnectorId(payload);

    for (Map<String, Object> meterValue : meterValues) {
      Object timestamp = meterValue.get("timestamp");
      String sampledAt = stringValue(timestamp);
      if (!sampledAt.isBlank()) {
        flattened.putIfAbsent("sampledAt", sampledAt);
      }

      String meterValueEvseId = resolveEvseId(meterValue);
      String meterValueConnectorId = resolveConnectorId(meterValue);
      String scopeType =
          resolveScopeType(
              firstNonBlank(meterValueConnectorId, payloadConnectorId),
              firstNonBlank(meterValueEvseId, payloadEvseId),
              payloadTransactionId);
      String scopeIdentifier =
          resolveScopeIdentifier(
              scopeType,
              firstNonBlank(meterValueConnectorId, payloadConnectorId),
              firstNonBlank(meterValueEvseId, payloadEvseId),
              payloadTransactionId);

      for (Map<String, Object> sampledValue : asObjectMapList(meterValue.get("sampledValue"))) {
        String metricName = stringValue(sampledValue.getOrDefault("measurand", "meter.value"));
        Double numericValue = parseNumeric(sampledValue.get("value"));
        if (numericValue != null) {
          flattened.put(metricName, numericValue);
          structuredSamples.add(
              buildStructuredSample(
                  metricName,
                  numericValue,
                  sampledAt,
                  scopeType,
                  scopeIdentifier,
                  sampledValue));
        }
      }
    }

    return new NormalizedSampleExtraction(flattened, structuredSamples);
  }

  private Map<String, Object> buildStructuredSample(
      String metricName,
      double metricValue,
      String sampledAt,
      String scopeType,
      String scopeIdentifier,
      Map<String, Object> sampledValue) {
    Map<String, Object> structured = new LinkedHashMap<>();
    structured.put("metricName", metricName);
    structured.put("metricValue", metricValue);
    if (!sampledAt.isBlank()) {
      structured.put("sampledAt", sampledAt);
    }
    structured.put("scopeType", scopeType);
    if (!scopeIdentifier.isBlank()) {
      structured.put("scopeIdentifier", scopeIdentifier);
    }
    copyIfPresent(structured, sampledValue, "unit");
    copyIfPresent(structured, sampledValue, "phase");
    copyIfPresent(structured, sampledValue, "location");
    copyIfPresent(structured, sampledValue, "context");
    return structured;
  }

  private void copyIfPresent(Map<String, Object> target, Map<String, Object> source, String key) {
    String value = stringValue(source.get(key));
    if (!value.isBlank()) {
      target.put(key, value);
    }
  }

  private String resolveScopeType(String connectorId, String evseId, String transactionId) {
    if (!connectorId.isBlank()) {
      return "connector";
    }
    if (!evseId.isBlank()) {
      return "evse";
    }
    if (!transactionId.isBlank()) {
      return "transaction";
    }
    return "station";
  }

  private String resolveScopeIdentifier(
      String scopeType, String connectorId, String evseId, String transactionId) {
    return switch (scopeType) {
      case "connector" -> connectorScopeIdentifier(connectorId, evseId);
      case "evse" -> evseId;
      case "transaction" -> transactionId;
      default -> "";
    };
  }

  private String connectorScopeIdentifier(String connectorId, String evseId) {
    if (connectorId.isBlank()) {
      return "";
    }
    if (evseId.isBlank()) {
      return connectorId;
    }
    return "evse-" + evseId + ":connector-" + connectorId;
  }

  private String resolveTransactionId(Map<String, Object> payload) {
    String direct = stringValue(payload.get("transactionId"));
    if (!direct.isBlank()) {
      return direct;
    }
    if (payload.get("transactionInfo") instanceof Map<?, ?> rawInfo) {
      return stringValue(rawInfo.get("transactionId"));
    }
    return "";
  }

  private String resolveEvseId(Map<String, Object> payload) {
    if (payload.get("evse") instanceof Map<?, ?> rawEvse) {
      String fromEvseObject = firstNonBlank(stringValue(rawEvse.get("id")), stringValue(rawEvse.get("evseId")));
      if (!fromEvseObject.isBlank()) {
        return fromEvseObject;
      }
    }
    return firstNonBlank(stringValue(payload.get("evseId")), stringValue(payload.get("evse")));
  }

  private String resolveConnectorId(Map<String, Object> payload) {
    if (payload.get("evse") instanceof Map<?, ?> rawEvse) {
      String fromEvseObject = stringValue(rawEvse.get("connectorId"));
      if (!fromEvseObject.isBlank()) {
        return fromEvseObject;
      }
    }
    return firstNonBlank(stringValue(payload.get("connectorId")), stringValue(payload.get("connector")));
  }

  private List<Map<String, Object>> asObjectMapList(Object raw) {
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }
    List<Map<String, Object>> result = new ArrayList<>();
    for (Object item : list) {
      if (item instanceof Map<?, ?> map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> typed = (Map<String, Object>) map;
        result.add(typed);
      }
    }
    return result;
  }

  private Double parseNumeric(Object rawValue) {
    if (rawValue instanceof Number number) {
      return number.doubleValue();
    }
    if (rawValue instanceof String text) {
      try {
        return Double.parseDouble(text);
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }

  private String stringValue(Object value) {
    return value == null ? "" : value.toString();
  }

  private record NormalizedSampleExtraction(
      Map<String, Object> flattenedValues, List<Map<String, Object>> structuredSamples) {}
}
