package com.arthexis.platform.ocpp;

import com.arthexis.platform.charging.ChargingStationService;
import com.arthexis.platform.telemetry.TelemetryIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
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
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    OcppMessage incoming = objectMapper.readValue(message.getPayload(), OcppMessage.class);

    if (incoming.payload() instanceof Map<?, ?> rawPayload) {
      @SuppressWarnings("unchecked")
      Map<String, Object> payload = (Map<String, Object>) rawPayload;

      if ("Heartbeat".equals(incoming.action())) {
        String stationId = (String) payload.getOrDefault("stationId", session.getId());
        chargingStationService.upsertStatus(stationId, "ONLINE");
        stateStore.storePendingCommand(stationId, incoming.messageId(), incoming.action());
      }

      if ("MeterValues".equals(incoming.action())) {
        String stationId = (String) payload.getOrDefault("stationId", session.getId());
        telemetryIngestionService.ingestMeterValues(stationId, payload);
      }
    }

    OcppMessage ack =
        new OcppMessage(
            "CALLRESULT", incoming.messageId(), incoming.action(), Map.of("status", "Accepted"));
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
  }
}
