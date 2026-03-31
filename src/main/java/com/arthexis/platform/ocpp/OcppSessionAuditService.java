package com.arthexis.platform.ocpp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class OcppSessionAuditService {

  private static final int PAYLOAD_SNAPSHOT_LIMIT = 8000;

  private final OcppSessionRecordRepository sessionRepository;
  private final OcppMessageRecordRepository messageRepository;
  private final ObjectMapper objectMapper;

  public OcppSessionAuditService(
      OcppSessionRecordRepository sessionRepository,
      OcppMessageRecordRepository messageRepository,
      ObjectMapper objectMapper) {
    this.sessionRepository = sessionRepository;
    this.messageRepository = messageRepository;
    this.objectMapper = objectMapper;
  }

  public void markSessionConnected(String sessionId) {
    Instant now = Instant.now();
    OcppSessionRecord session =
        sessionRepository
            .findBySessionId(sessionId)
            .orElseGet(() -> new OcppSessionRecord(sessionId, now));
    session.setDisconnectedAt(null);
    session.setUpdatedAt(now);
    sessionRepository.save(session);
  }

  public void markSessionDisconnected(String sessionId) {
    sessionRepository
        .findBySessionId(sessionId)
        .ifPresent(
            existing -> {
              Instant now = Instant.now();
              existing.setDisconnectedAt(now);
              existing.setUpdatedAt(now);
              sessionRepository.save(existing);
            });
  }

  public void recordIncomingParsed(String sessionId, OcppMessage incoming, String stationId, String rawPayload) {
    recordMessage(
        sessionId,
        stationId,
        "INBOUND",
        incoming.messageType(),
        incoming.action(),
        incoming.messageId(),
        rawPayload,
        "PARSED",
        null);
  }

  public void recordIncomingParseFailure(String sessionId, String rawPayload) {
    recordMessage(
        sessionId,
        null,
        "INBOUND",
        null,
        null,
        null,
        rawPayload,
        "PARSE_FAILED",
        "InvalidJson");
  }

  public void recordOutgoingCallResult(
      String sessionId, OcppMessage outgoing, String stationId, String resultStatus) {
    recordMessage(
        sessionId,
        stationId,
        "OUTBOUND",
        outgoing.messageType(),
        outgoing.action(),
        outgoing.messageId(),
        payloadToString(outgoing.payload()),
        "PARSED",
        resultStatus);
  }

  private void recordMessage(
      String sessionId,
      String stationId,
      String direction,
      String messageType,
      String action,
      String messageId,
      String payload,
      String parseStatus,
      String resultStatus) {
    Instant now = Instant.now();
    OcppSessionRecord session =
        sessionRepository
            .findBySessionId(sessionId)
            .orElseGet(() -> new OcppSessionRecord(sessionId, now));

    if (stationId != null && !stationId.isBlank()) {
      session.setStationId(stationId);
    }
    session.setLastMessageAt(now);
    session.setUpdatedAt(now);
    OcppSessionRecord persistedSession = sessionRepository.save(session);

    Snapshot snapshot = truncate(payload);
    messageRepository.save(
        new OcppMessageRecord(
            persistedSession,
            sessionId,
            session.getStationId(),
            direction,
            messageType,
            action,
            messageId,
            snapshot.value(),
            snapshot.truncated(),
            parseStatus,
            resultStatus,
            now,
            now));
  }

  private String payloadToString(Object payload) {
    if (payload == null) {
      return "{}";
    }
    if (payload instanceof String textPayload) {
      return textPayload;
    }
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException ex) {
      return String.valueOf(payload);
    }
  }

  private Snapshot truncate(String payload) {
    String normalized = Optional.ofNullable(payload).orElse("{}");
    if (normalized.length() <= PAYLOAD_SNAPSHOT_LIMIT) {
      return new Snapshot(normalized, false);
    }
    return new Snapshot(normalized.substring(0, PAYLOAD_SNAPSHOT_LIMIT), true);
  }

  private record Snapshot(String value, boolean truncated) {}
}
