package com.arthexis.platform.simulator;

import com.arthexis.platform.ocpp.OcppMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@ConditionalOnProperty(prefix = "arthexis.ocpp.simulator", name = "enabled", havingValue = "true")
public class OcppChargePointSimulator {

  private static final Logger log = LoggerFactory.getLogger(OcppChargePointSimulator.class);

  private final WebSocketClient webSocketClient;
  private final OcppSimulatorProperties properties;
  private final ObjectMapper objectMapper;

  private final AtomicReference<WebSocketSession> session = new AtomicReference<>();
  private final AtomicBoolean bootSent = new AtomicBoolean(false);
  private final AtomicBoolean connectionAttemptInFlight = new AtomicBoolean(false);

  public OcppChargePointSimulator(
      WebSocketClient webSocketClient, OcppSimulatorProperties properties, ObjectMapper objectMapper) {
    this.webSocketClient = webSocketClient;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Scheduled(
      initialDelayString = "${arthexis.ocpp.simulator.reconnect-delay:10s}",
      fixedDelayString = "${arthexis.ocpp.simulator.reconnect-delay:10s}")
  void ensureConnected() {
    WebSocketSession existing = session.get();
    if ((existing != null && existing.isOpen()) || !connectionAttemptInFlight.compareAndSet(false, true)) {
      return;
    }

    try {
      String target = URI.create(properties.getCsmsUrl()).toString();
      webSocketClient.execute(buildHandler(), target).get();
      log.info("OCPP simulator connected to {} as {}", target, properties.getChargePointId());
    } catch (Exception ex) {
      log.warn("OCPP simulator connection attempt failed: {}", ex.getMessage());
      session.set(null);
      bootSent.set(false);
    } finally {
      connectionAttemptInFlight.set(false);
    }
  }

  @Scheduled(fixedDelayString = "${arthexis.ocpp.simulator.heartbeat-interval:60s}")
  void sendHeartbeatIfConnected() {
    WebSocketSession current = session.get();
    if (current == null || !current.isOpen()) {
      return;
    }

    if (!bootSent.get()) {
      sendBootNotification(current);
      return;
    }

    sendCall(
        current,
        "Heartbeat",
        Map.of("timestamp", Instant.now().toString(), "chargePointId", properties.getChargePointId()));
  }

  private WebSocketHandler buildHandler() {
    return new TextWebSocketHandler() {
      @Override
      public void afterConnectionEstablished(WebSocketSession webSocketSession) {
        session.set(webSocketSession);
        bootSent.set(false);
        sendBootNotification(webSocketSession);
      }

      @Override
      protected void handleTextMessage(WebSocketSession webSocketSession, TextMessage message)
          throws IOException {
        OcppMessage response = objectMapper.readValue(message.getPayload(), OcppMessage.class);
        log.debug(
            "OCPP simulator received {} for action {}", response.messageType(), response.action());
      }

      @Override
      public void afterConnectionClosed(WebSocketSession webSocketSession, CloseStatus closeStatus) {
        session.compareAndSet(webSocketSession, null);
        bootSent.set(false);
        log.info("OCPP simulator disconnected: {}", closeStatus);
      }

      @Override
      public void handleTransportError(WebSocketSession webSocketSession, Throwable exception) {
        session.compareAndSet(webSocketSession, null);
        bootSent.set(false);
        log.warn("OCPP simulator transport error: {}", exception.getMessage());
      }
    };
  }

  private void sendBootNotification(WebSocketSession current) {
    sendCall(
        current,
        "BootNotification",
        Map.of(
            "chargePointId",
            properties.getChargePointId(),
            "vendor",
            "Arthexis",
            "model",
            "Simulator",
            "simulated",
            true));
    bootSent.set(true);
  }

  private void sendCall(WebSocketSession current, String action, Map<String, Object> payload) {
    OcppMessage message = new OcppMessage("CALL", UUID.randomUUID().toString(), action, payload);
    try {
      current.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
      log.debug("OCPP simulator sent {} as {}", action, properties.getChargePointId());
    } catch (JsonProcessingException ex) {
      log.warn("OCPP simulator could not serialize {}: {}", action, ex.getMessage());
    } catch (IOException ex) {
      log.warn("OCPP simulator could not send {}: {}", action, ex.getMessage());
      session.compareAndSet(current, null);
      bootSent.set(false);
    }
  }
}
