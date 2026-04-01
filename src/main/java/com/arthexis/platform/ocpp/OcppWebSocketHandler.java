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
  private final OcppCommandDispatchService commandDispatchService;
  private final OcppOutboundSessionRouter outboundSessionRouter;
  private final OcppSessionStateStore stateStore;

  public OcppWebSocketHandler(
      ObjectMapper objectMapper,
      OcaOcppBridgeService ocaOcppBridgeService,
      OcppSessionAuditService sessionAuditService,
      OcppCommandDispatchService commandDispatchService,
      OcppOutboundSessionRouter outboundSessionRouter,
      OcppSessionStateStore stateStore) {
    this.objectMapper = objectMapper;
    this.ocaOcppBridgeService = ocaOcppBridgeService;
    this.sessionAuditService = sessionAuditService;
    this.commandDispatchService = commandDispatchService;
    this.outboundSessionRouter = outboundSessionRouter;
    this.stateStore = stateStore;
  }

  @Override
  public void afterConnectionEstablished(WebSocketSession session) {
    outboundSessionRouter.registerSession(session);
    sessionAuditService.markSessionConnected(session.getId());
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    outboundSessionRouter.unRegisterSession(session);
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

    if ("CALLRESULT".equals(incoming.messageType())) {
      commandDispatchService.acknowledgeByMessageId(incoming.messageId());
      sessionAuditService.recordIncomingParsed(session.getId(), incoming, null, message.getPayload());
      return;
    }

    if ("CALLERROR".equals(incoming.messageType())) {
      String errorDetail = incoming.payload() == null ? "Error" : incoming.payload().toString();
      commandDispatchService.failByMessageId(incoming.messageId(), errorDetail);
      sessionAuditService.recordIncomingCallError(
          session.getId(), incoming, null, message.getPayload(), errorDetail);
      return;
    }

    OcppBridgeResponse bridgeResponse = ocaOcppBridgeService.handleIncoming(session.getId(), incoming);
    outboundSessionRouter.bindStationToSession(bridgeResponse.stationId(), session.getId());
    stateStore.bindStationSession(bridgeResponse.stationId(), session.getId());
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
