package com.arthexis.platform.ocpp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class OcppWebSocketHandler extends TextWebSocketHandler {

  private final ObjectMapper objectMapper;
  private final OcaOcppBridgeService ocaOcppBridgeService;

  public OcppWebSocketHandler(ObjectMapper objectMapper, OcaOcppBridgeService ocaOcppBridgeService) {
    this.objectMapper = objectMapper;
    this.ocaOcppBridgeService = ocaOcppBridgeService;
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    OcppMessage incoming = objectMapper.readValue(message.getPayload(), OcppMessage.class);
    OcppMessage ack =
        new OcppMessage(
            "CALLRESULT",
            incoming.messageId(),
            incoming.action(),
            ocaOcppBridgeService.handleIncoming(session.getId(), incoming));
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
  }
}
