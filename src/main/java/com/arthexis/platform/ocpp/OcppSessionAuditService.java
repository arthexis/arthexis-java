package com.arthexis.platform.ocpp;

import com.arthexis.platform.app.admin.ConnectorChangedEvent;
import com.arthexis.platform.app.admin.OcppMessagePersistedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class OcppSessionAuditService {

  private static final int PAYLOAD_SNAPSHOT_LIMIT = 8000;
  private static final int RESULT_STATUS_MAX_LENGTH = 64;

  private final OcppSessionRecordRepository sessionRepository;
  private final OcppMessageRecordRepository messageRepository;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;

  public OcppSessionAuditService(
      OcppSessionRecordRepository sessionRepository,
      OcppMessageRecordRepository messageRepository,
      ObjectMapper objectMapper,
      ApplicationEventPublisher eventPublisher) {
    this.sessionRepository = sessionRepository;
    this.messageRepository = messageRepository;
    this.objectMapper = objectMapper;
    this.eventPublisher = eventPublisher;
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

    if ("StatusNotification".equals(incoming.action())) {
      publishConnectorChanged(stationId, incoming.payload());
    }
  }

  public void recordIncomingCallError(
      String sessionId, OcppMessage incoming, String stationId, String rawPayload, String errorDetail) {
    recordMessage(
        sessionId,
        stationId,
        "INBOUND",
        incoming.messageType(),
        incoming.action(),
        incoming.messageId(),
        rawPayload,
        "PARSED",
        truncateResultStatus(errorDetail == null || errorDetail.isBlank() ? "Error" : errorDetail.trim()));
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

    eventPublisher.publishEvent(
        new OcppMessagePersistedEvent(
            session.getStationId(),
            sessionId,
            direction,
            action,
            parseStatus,
            resultStatus,
            now));
  }

  private void publishConnectorChanged(String stationId, Object payload) {
    if (!(payload instanceof Map<?, ?> rawMap)) {
      return;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> map = (Map<String, Object>) rawMap;
    Map<String, Object> evse = mapValue(map.get("evse"));

    eventPublisher.publishEvent(
        new ConnectorChangedEvent(
            stationId,
            intValue(firstNonNull(evse.get("id"), map.get("evseId"))),
            intValue(firstNonNull(evse.get("connectorId"), map.get("connectorId"), map.get("connector"))),
            stringValue(firstNonNull(map.get("status"), map.get("connectorStatus"))),
            stringValue(map.get("connectorType")),
            stringValue(firstNonNull(map.get("availability"), map.get("connectorAvailability"))),
            Instant.now()));
  }

  private Map<String, Object> mapValue(Object value) {
    if (value instanceof Map<?, ?> rawMap) {
      @SuppressWarnings("unchecked")
      Map<String, Object> casted = (Map<String, Object>) rawMap;
      return casted;
    }
    return Map.of();
  }

  private Integer intValue(Object value) {
    if (value instanceof Number number) {
      return number.intValue();
    }
    if (value == null) {
      return null;
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private String stringValue(Object value) {
    return value == null ? null : value.toString();
  }

  private Object firstNonNull(Object... values) {
    for (Object value : values) {
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String truncateResultStatus(String resultStatus) {
    if (resultStatus.length() <= RESULT_STATUS_MAX_LENGTH) {
      return resultStatus;
    }
    return resultStatus.substring(0, RESULT_STATUS_MAX_LENGTH - 3) + "...";
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
