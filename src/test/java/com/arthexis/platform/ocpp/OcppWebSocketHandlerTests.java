package com.arthexis.platform.ocpp;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class OcppWebSocketHandlerTests {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private OcaOcppBridgeService bridgeService;
  @Mock private OcppSessionAuditService auditService;
  @Mock private OcppCommandDispatchService commandDispatchService;
  @Mock private OcppOutboundSessionRouter outboundSessionRouter;
  @Mock private OcppSessionStateStore stateStore;
  @Mock private WebSocketSession session;

  private OcppWebSocketHandler handler;

  @BeforeEach
  void setUp() {
    handler =
        new OcppWebSocketHandler(
            objectMapper,
            bridgeService,
            auditService,
            commandDispatchService,
            outboundSessionRouter,
            stateStore);
    when(session.getId()).thenReturn("session-1");
  }

  @Test
  void routesCallErrorToCommandFailureAndAudit() throws Exception {
    OcppMessage callError =
        new OcppMessage(
            "CALLERROR",
            "msg-123",
            "Reset",
            Map.of("errorCode", "ProtocolError", "errorDescription", "Rejected by charger"));
    String payload = objectMapper.writeValueAsString(callError);

    handler.handleTextMessage(session, new TextMessage(payload));

    verify(commandDispatchService)
        .failByMessageId(
            "msg-123",
            argThat(
                reason ->
                    reason.contains("ProtocolError") && reason.contains("Rejected by charger")));
    verify(auditService)
        .recordIncomingCallError(
            "session-1",
            callError,
            null,
            payload,
            argThat(
                reason ->
                    reason.contains("ProtocolError") && reason.contains("Rejected by charger")));
    verify(bridgeService, never()).handleIncoming("session-1", callError);
  }
}
