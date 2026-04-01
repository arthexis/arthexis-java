package com.arthexis.platform.ocpp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OcppCommandTranslator {

  public TranslatedCommand translate(String profile, String action, Map<String, Object> payload) {
    String normalizedProfile = normalizeProfile(profile);
    String normalizedAction = normalizeAction(action);
    Map<String, Object> sourcePayload = payload == null ? Map.of() : payload;

    if (normalizedProfile.contains("2")) {
      return translateOcpp2x(normalizedAction, sourcePayload, action);
    }
    return translateOcpp16(normalizedAction, sourcePayload, action);
  }

  private TranslatedCommand translateOcpp16(
      String normalizedAction, Map<String, Object> sourcePayload, String fallbackAction) {
    return switch (normalizedAction) {
      case "start", "remotestarttransaction" ->
          new TranslatedCommand("RemoteStartTransaction", ocpp16StartPayload(sourcePayload));
      case "stop", "remotestoptransaction" ->
          new TranslatedCommand("RemoteStopTransaction", ocpp16StopPayload(sourcePayload));
      case "reset" -> new TranslatedCommand("Reset", ocpp16ResetPayload(sourcePayload));
      case "availability", "changeavailability" ->
          new TranslatedCommand("ChangeAvailability", ocpp16AvailabilityPayload(sourcePayload));
      default -> new TranslatedCommand(trimOrDefault(fallbackAction, "Unknown"), sourcePayload);
    };
  }

  private TranslatedCommand translateOcpp2x(
      String normalizedAction, Map<String, Object> sourcePayload, String fallbackAction) {
    return switch (normalizedAction) {
      case "start", "requeststarttransaction" ->
          new TranslatedCommand("RequestStartTransaction", ocpp2xStartPayload(sourcePayload));
      case "stop", "requeststoptransaction" ->
          new TranslatedCommand("RequestStopTransaction", ocpp2xStopPayload(sourcePayload));
      case "reset" -> new TranslatedCommand("Reset", ocpp2xResetPayload(sourcePayload));
      case "availability", "changeavailability" ->
          new TranslatedCommand("ChangeAvailability", ocpp2xAvailabilityPayload(sourcePayload));
      default -> new TranslatedCommand(trimOrDefault(fallbackAction, "Unknown"), sourcePayload);
    };
  }

  private Map<String, Object> ocpp16StartPayload(Map<String, Object> sourcePayload) {
    Map<String, Object> translated = new LinkedHashMap<>();
    Integer connectorId = extractConnectorId(sourcePayload);
    if (connectorId != null) {
      translated.put("connectorId", connectorId);
    }
    String idTag = extractToken(sourcePayload);
    if (idTag != null) {
      translated.put("idTag", idTag);
    }
    Object chargingProfile = sourcePayload.get("chargingProfile");
    if (chargingProfile != null) {
      translated.put("chargingProfile", chargingProfile);
    }
    return translated;
  }

  private Map<String, Object> ocpp2xStartPayload(Map<String, Object> sourcePayload) {
    Map<String, Object> translated = new LinkedHashMap<>();
    Object remoteStartId = sourcePayload.get("remoteStartId");
    if (remoteStartId != null) {
      translated.put("remoteStartId", remoteStartId);
    }
    Integer evseId = extractEvseId(sourcePayload);
    if (evseId != null) {
      translated.put("evseId", evseId);
    }
    String token = extractToken(sourcePayload);
    if (token != null) {
      translated.put(
          "idToken",
          Map.of(
              "idToken", token,
              "type", trimOrDefault(asString(sourcePayload.get("tokenType")), "Central")));
    }
    Object chargingProfile = sourcePayload.get("chargingProfile");
    if (chargingProfile != null) {
      translated.put("chargingProfile", chargingProfile);
    }
    return translated;
  }

  private Map<String, Object> ocpp16StopPayload(Map<String, Object> sourcePayload) {
    Map<String, Object> translated = new LinkedHashMap<>();
    Object transactionId = sourcePayload.get("transactionId");
    if (transactionId != null) {
      translated.put("transactionId", transactionId);
    }
    return translated;
  }

  private Map<String, Object> ocpp2xStopPayload(Map<String, Object> sourcePayload) {
    return ocpp16StopPayload(sourcePayload);
  }

  private Map<String, Object> ocpp16ResetPayload(Map<String, Object> sourcePayload) {
    return Map.of("type", trimOrDefault(asString(sourcePayload.get("type")), "Soft"));
  }

  private Map<String, Object> ocpp2xResetPayload(Map<String, Object> sourcePayload) {
    return Map.of("type", trimOrDefault(asString(sourcePayload.get("type")), "Immediate"));
  }

  private Map<String, Object> ocpp16AvailabilityPayload(Map<String, Object> sourcePayload) {
    Map<String, Object> translated = new LinkedHashMap<>();
    Integer connectorId = extractConnectorId(sourcePayload);
    if (connectorId != null) {
      translated.put("connectorId", connectorId);
    }
    String type = asString(sourcePayload.get("type"));
    if (type == null) {
      type = asString(sourcePayload.get("operationalStatus"));
    }
    translated.put("type", trimOrDefault(type, "Operative"));
    return translated;
  }

  private Map<String, Object> ocpp2xAvailabilityPayload(Map<String, Object> sourcePayload) {
    Map<String, Object> translated = new LinkedHashMap<>();
    Integer evseId = extractEvseId(sourcePayload);
    Integer connectorId = extractConnectorId(sourcePayload);
    if (evseId != null || connectorId != null) {
      Map<String, Object> evse = new LinkedHashMap<>();
      if (evseId != null) {
        evse.put("id", evseId);
      }
      if (connectorId != null) {
        evse.put("connectorId", connectorId);
      }
      translated.put("evse", evse);
    }
    String operationalStatus = asString(sourcePayload.get("operationalStatus"));
    if (operationalStatus == null) {
      operationalStatus = asString(sourcePayload.get("type"));
    }
    translated.put("operationalStatus", trimOrDefault(operationalStatus, "Operative"));
    return translated;
  }

  private Integer extractConnectorId(Map<String, Object> payload) {
    Integer connectorId = asInteger(payload.get("connectorId"));
    if (connectorId == null) {
      connectorId = asInteger(payload.get("connector"));
    }
    return connectorId;
  }

  private Integer extractEvseId(Map<String, Object> payload) {
    Integer evseId = asInteger(payload.get("evseId"));
    if (evseId == null) {
      evseId = extractConnectorId(payload);
    }
    return evseId;
  }

  private String extractToken(Map<String, Object> payload) {
    String token = asString(payload.get("idTag"));
    if (token == null) {
      token = asString(payload.get("token"));
    }
    if (token == null) {
      token = asString(payload.get("idToken"));
    }
    Object nestedIdToken = payload.get("idToken");
    if (token == null && nestedIdToken instanceof Map<?, ?> nested) {
      token = asString(nested.get("idToken"));
    }
    return token;
  }

  private Integer asInteger(Object value) {
    if (value instanceof Integer integerValue) {
      return integerValue;
    }
    if (value instanceof Number numericValue) {
      return numericValue.intValue();
    }
    if (value instanceof String textValue && !textValue.isBlank()) {
      try {
        return Integer.parseInt(textValue.trim());
      } catch (NumberFormatException ignored) {
        return null;
      }
    }
    return null;
  }

  private String asString(Object value) {
    if (value instanceof String textValue && !textValue.isBlank()) {
      return textValue.trim();
    }
    return null;
  }

  private String trimOrDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private String normalizeAction(String action) {
    return action == null ? "" : action.trim().toLowerCase();
  }

  private String normalizeProfile(String profile) {
    return profile == null ? "python-ocpp16" : profile.trim().toLowerCase();
  }

  public record TranslatedCommand(String action, Map<String, Object> payload) {}
}
