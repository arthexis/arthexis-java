package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OcppSessionAuditServiceTests {

  @Mock private OcppSessionRecordRepository sessionRepository;
  @Mock private OcppMessageRecordRepository messageRepository;
  @Mock private ApplicationEventPublisher eventPublisher;

  private OcppSessionAuditService service;

  @BeforeEach
  void setUp() {
    service =
        new OcppSessionAuditService(
            sessionRepository, messageRepository, new ObjectMapper(), eventPublisher);
  }

  @Test
  void storesCallErrorWithMessageMetadataAndErrorDetail() {
    OcppSessionRecord sessionRecord = new OcppSessionRecord("session-1", Instant.now());
    sessionRecord.setStationId("CP-100");

    when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.of(sessionRecord));
    when(sessionRepository.save(any(OcppSessionRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

    OcppMessage callError =
        new OcppMessage(
            "CALLERROR",
            "msg-321",
            "Reset",
            Map.of("errorCode", "ProtocolError", "errorDescription", "Rejected by charger"));
    String rawPayload =
        "{\"messageType\":\"CALLERROR\",\"messageId\":\"msg-321\",\"action\":\"Reset\",\"payload\":{\"errorCode\":\"ProtocolError\"}}";

    service.recordIncomingCallError(
        "session-1", callError, "CP-100", rawPayload, "ProtocolError: Rejected by charger");

    ArgumentCaptor<OcppMessageRecord> captor = ArgumentCaptor.forClass(OcppMessageRecord.class);
    verify(messageRepository).save(captor.capture());

    OcppMessageRecord saved = captor.getValue();
    assertThat(saved.getDirection()).isEqualTo("INBOUND");
    assertThat(saved.getMessageType()).isEqualTo("CALLERROR");
    assertThat(saved.getAction()).isEqualTo("Reset");
    assertThat(saved.getMessageId()).isEqualTo("msg-321");
    assertThat(saved.getParseStatus()).isEqualTo("PARSED");
    assertThat(saved.getResultStatus()).isEqualTo("ProtocolError: Rejected by charger");
    assertThat(saved.getPayloadSnapshot()).isEqualTo(rawPayload);
  }
}
