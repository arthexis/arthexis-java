package com.arthexis.platform.ocpp;

import com.arthexis.platform.charging.ChargingStationService;
import com.arthexis.platform.telemetry.TelemetryIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class OcppWebSocketHandler extends TextWebSocketHandler {

  private final ObjectMapper objectMapper;
  private final ChargingStationService chargingStationService;
  private final OcppSessionStateStore stateStore;
  private final TelemetryIngestionService telemetryIngestionService;

  public OcppWebSocketHandler(
      ObjectMapper objectMapper,
      ChargingStationService chargingStationService,
      OcppSessionStateStore stateStore,
      TelemetryIngestionService telemetryIngestionService) {
    this.objectMapper = objectMapper;
    this.chargingStationService = chargingStationService;
    this.stateStore = stateStore;
    this.telemetryIngestionService = telemetryIngestionService;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    String stationId = resolveStationId(session);
    chargingStationService.upsertStatus(stationId, "ONLINE");
    stateStore.storeActiveSession(stationId, session.getId());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    String stationId = resolveStationId(session);
    chargingStationService.markOffline(stationId);
    stateStore.removeActiveSession(stationId);
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    OcppMessage incoming = objectMapper.readValue(message.getPayload(), OcppMessage.class);

    if (incoming.payload() instanceof Map<?, ?> rawPayload) {
      @SuppressWarnings("unchecked")
      Map<String, Object> payload = (Map<String, Object>) rawPayload;

      if ("Heartbeat".equals(incoming.action())) {
        String stationId = (String) payload.getOrDefault("stationId", resolveStationId(session));
        chargingStationService.upsertStatus(stationId, "ONLINE");
        stateStore.storePendingCommand(stationId, incoming.messageId(), incoming.action());
      }

      if ("MeterValues".equals(incoming.action())) {
        String stationId = (String) payload.getOrDefault("stationId", resolveStationId(session));
        telemetryIngestionService.ingestMeterValues(stationId, payload);
      }
    }

    OcppMessage ack =
        new OcppMessage(
            "CALLRESULT", incoming.messageId(), incoming.action(), Map.of("status", "Accepted"));
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
  }

  private String resolveStationId(WebSocketSession session) {
    URI sessionUri = session.getUri();
    if (sessionUri == null) {
      return session.getId();
    }

    String stationIdFromPath = extractFromPath(sessionUri.getPath());
    if (stationIdFromPath != null) {
      return stationIdFromPath;
    }

    String stationIdFromQuery = extractFromQuery(sessionUri.getQuery());
    if (stationIdFromQuery != null) {
      return stationIdFromQuery;
    }

    return session.getId();
  }

  private String extractFromPath(String path) {
    if (path == null || !path.startsWith("/ws/ocpp/")) {
      return null;
    }
    String stationId = path.substring("/ws/ocpp/".length()).trim();
    return stationId.isEmpty() ? null : stationId;
  }

  private String extractFromQuery(String query) {
    if (query == null || query.isBlank()) {
      return null;
    }

    return Arrays.stream(query.split("&"))
        .map(entry -> entry.split("=", 2))
        .filter(parts -> parts.length == 2)
        .filter(parts -> Objects.equals(parts[0], "chargePointId") || Objects.equals(parts[0], "stationId"))
        .map(parts -> parts[1])
        .filter(value -> !value.isBlank())
        .findFirst()
        .orElse(null);
  }
}
