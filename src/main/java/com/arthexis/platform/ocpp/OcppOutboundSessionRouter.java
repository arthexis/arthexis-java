package com.arthexis.platform.ocpp;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class OcppOutboundSessionRouter {

  private final OcppFrameCodec frameCodec;
  private final Map<String, WebSocketSession> sessionsBySessionId = new ConcurrentHashMap<>();
  private final Map<String, String> sessionIdByStationId = new ConcurrentHashMap<>();

  public OcppOutboundSessionRouter(OcppFrameCodec frameCodec) {
    this.frameCodec = frameCodec;
  }

  public void registerSession(WebSocketSession session) {
    sessionsBySessionId.put(session.getId(), session);
  }

  public void unRegisterSession(WebSocketSession session) {
    sessionsBySessionId.remove(session.getId());
    sessionIdByStationId.entrySet().removeIf(entry -> entry.getValue().equals(session.getId()));
  }

  public void bindStationToSession(String stationId, String sessionId) {
    if (stationId == null || stationId.isBlank()) {
      return;
    }
    sessionIdByStationId.put(stationId, sessionId);
  }

  public void sendToStation(String stationId, OcppMessage message) throws IOException {
    String sessionId = sessionIdByStationId.get(stationId);
    if (sessionId == null) {
      throw new IOException("No active websocket session for station " + stationId);
    }
    WebSocketSession session = sessionsBySessionId.get(sessionId);
    if (session == null || !session.isOpen()) {
      throw new IOException("Resolved websocket session is unavailable for station " + stationId);
    }
    session.sendMessage(new TextMessage(frameCodec.encode(message)));
  }
}
