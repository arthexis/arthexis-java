package com.arthexis.platform.ocpp;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
  private final OcppFrameCodec frameCodec = new OcppFrameCodec(objectMapper);

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
            frameCodec,
            bridgeService,
            auditService,
            commandDispatchService,
            outboundSessionRouter,
            stateStore);
    when(session.getId()).thenReturn("session-1");
  }

  @Test
  void routesCallErrorToCommandFailureAndAudit() throws Exception {
    String payload =
        """
        [4,"msg-123","ProtocolError","Rejected by charger",{"vendorCode":"X1"}]
        """;

    handler.handleTextMessage(session, new TextMessage(payload));
    OcppMessage callError =
        new OcppMessage(
            "CALLERROR",
            "msg-123",
            null,
            Map.of(
                "errorCode", "ProtocolError",
                "errorDescription", "Rejected by charger",
                "errorDetails", Map.of("vendorCode", "X1")));

    verify(commandDispatchService)
        .failByMessageId(
            eq("msg-123"),
            argThat(
                reason ->
                    reason.contains("ProtocolError") && reason.contains("Rejected by charger")));
    verify(auditService)
        .recordIncomingCallError(
            eq("session-1"),
            eq(callError),
            isNull(),
            eq(payload),
            argThat(
                reason ->
                    reason.contains("ProtocolError") && reason.contains("Rejected by charger")));
    verify(bridgeService, never()).handleIncoming(eq("session-1"), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsUnknownMessageTypeId() {
    String payload = "[9,\"msg-unknown\",\"Ignored\",{}]";

    assertThatThrownBy(() -> handler.handleTextMessage(session, new TextMessage(payload)))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Unknown OCPP message type id");

    verify(auditService).recordIncomingParseFailure("session-1", payload);
    verify(bridgeService, never()).handleIncoming(eq("session-1"), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsMalformedCallLength() {
    String payload = "[2,\"msg-short\",\"Heartbeat\"]";

    assertThatThrownBy(() -> handler.handleTextMessage(session, new TextMessage(payload)))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("CALL frame must have 4 elements");

    verify(auditService).recordIncomingParseFailure("session-1", payload);
    verify(bridgeService, never()).handleIncoming(eq("session-1"), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsInvalidCallFieldTypes() {
    String payload = "[2,123,\"Heartbeat\",{}]";

    assertThatThrownBy(() -> handler.handleTextMessage(session, new TextMessage(payload)))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("CALL uniqueId must be a string");

    verify(auditService).recordIncomingParseFailure("session-1", payload);
    verify(bridgeService, never()).handleIncoming(eq("session-1"), org.mockito.ArgumentMatchers.any());
  }
}
