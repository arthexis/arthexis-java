package com.arthexis.platform.ocpp;

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
    normalized.putAll(extractSampledValues(payload));
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

    normalized.putAll(extractSampledValues(payload));
    if (!normalized.containsKey("sampledAt")) {
      normalized.put("sampledAt", Instant.now().toString());
    }
    return normalized;
  }

  private Map<String, Object> extractSampledValues(Map<String, Object> payload) {
    Map<String, Object> flattened = new LinkedHashMap<>();
    List<Map<String, Object>> meterValues = asObjectMapList(payload.get("meterValue"));
    if (meterValues.isEmpty()) {
      meterValues = asObjectMapList(payload.get("meterValues"));
    }

    for (Map<String, Object> meterValue : meterValues) {
      Object timestamp = meterValue.get("timestamp");
      if (timestamp instanceof String text) {
        flattened.putIfAbsent("sampledAt", text);
      }

      for (Map<String, Object> sampledValue : asObjectMapList(meterValue.get("sampledValue"))) {
        String metricName = stringValue(sampledValue.getOrDefault("measurand", "meter.value"));
        Object rawValue = sampledValue.get("value");
        Double numericValue = parseNumeric(rawValue);
        if (numericValue != null) {
          flattened.put(metricName, numericValue);
        }
      }
    }

    return flattened;
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

  private String stringValue(Object value) {
    return value == null ? "" : value.toString();
  }
}
