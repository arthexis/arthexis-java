package com.arthexis.platform.ocpp;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class OcppWebSocketHandler extends TextWebSocketHandler {

  private final ObjectMapper objectMapper;
  private final OcaOcppBridgeService ocaOcppBridgeService;
  private final OcppSessionAuditService sessionAuditService;

  public OcppWebSocketHandler(
      ObjectMapper objectMapper,
      OcaOcppBridgeService ocaOcppBridgeService,
      OcppSessionAuditService sessionAuditService) {
    this.objectMapper = objectMapper;
    this.ocaOcppBridgeService = ocaOcppBridgeService;
    this.sessionAuditService = sessionAuditService;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    sessionAuditService.markSessionConnected(session.getId());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    sessionAuditService.markSessionDisconnected(session.getId());
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    OcppMessage incoming;
    try {
      incoming = objectMapper.readValue(message.getPayload(), OcppMessage.class);
    } catch (IOException ex) {
      sessionAuditService.recordIncomingParseFailure(session.getId(), message.getPayload());
      throw ex;
    }

    OcppBridgeResponse bridgeResponse = ocaOcppBridgeService.handleIncoming(session.getId(), incoming);
    sessionAuditService.recordIncomingParsed(
        session.getId(), incoming, bridgeResponse.stationId(), message.getPayload());

    OcppMessage ack =
        new OcppMessage(
            "CALLRESULT", incoming.messageId(), incoming.action(), bridgeResponse.payload());
    sessionAuditService.recordOutgoingCallResult(
        session.getId(), ack, bridgeResponse.stationId(), bridgeResponse.resultStatus());

    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
  }
}
