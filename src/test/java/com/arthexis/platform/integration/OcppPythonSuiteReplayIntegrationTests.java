package com.arthexis.platform.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.arthexis.platform.charging.ChargingConnectorStateRepository;
import com.arthexis.platform.charging.ChargingConnectorStateService;
import com.arthexis.platform.charging.ChargingStation;
import com.arthexis.platform.charging.ChargingStationRepository;
import com.arthexis.platform.charging.ChargingStationService;
import com.arthexis.platform.auth.AuthorizationDecision;
import com.arthexis.platform.ocpp.OcaOcppBridgeService;
import com.arthexis.platform.ocpp.OcaOcppPayloadNormalizer;
import com.arthexis.platform.ocpp.OcppMessage;
import com.arthexis.platform.ocpp.OcppMessageRecordRepository;
import com.arthexis.platform.ocpp.OcppCommandDispatchProperties;
import com.arthexis.platform.ocpp.OcppCommandDispatchService;
import com.arthexis.platform.ocpp.OcppFrameCodec;
import com.arthexis.platform.ocpp.OcppCommandRecordRepository;
import com.arthexis.platform.ocpp.OcppOutboundSessionRouter;
import com.arthexis.platform.ocpp.OcppSessionAuditService;
import com.arthexis.platform.ocpp.OcppSessionRecordRepository;
import com.arthexis.platform.ocpp.OcppSessionStateStore;
import com.arthexis.platform.ocpp.OcppWebSocketHandler;
import com.arthexis.platform.telemetry.TelemetryIngestionService;
import com.arthexis.platform.telemetry.TelemetrySampleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

@DataJpaTest
class OcppPythonSuiteReplayIntegrationTests {

  @Autowired private ChargingStationRepository chargingStationRepository;

  @Autowired private ChargingConnectorStateRepository connectorStateRepository;

  @Autowired private TelemetrySampleRepository telemetrySampleRepository;

  @Autowired private OcppSessionRecordRepository sessionRecordRepository;

  @Autowired private OcppMessageRecordRepository messageRecordRepository;

  @Autowired private OcppCommandRecordRepository commandRecordRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  private OcppWebSocketHandler webSocketHandler;
  private ObjectMapper objectMapper;
  private OcppFrameCodec frameCodec;

  @BeforeEach
  void setUp() {
    ApplicationEventPublisher noOpEvents = event -> {};
    objectMapper = new ObjectMapper();
    frameCodec = new OcppFrameCodec(objectMapper);
    OcaOcppBridgeService bridgeService =
        new OcaOcppBridgeService(
            new ChargingStationService(chargingStationRepository, noOpEvents),
            new ChargingConnectorStateService(connectorStateRepository),
            new OcppSessionStateStore(null) {
              @Override
              public void storePendingCommand(String stationId, String commandId, String action) {}
            },
            new TelemetryIngestionService(telemetrySampleRepository, noOpEvents),
            new OcaOcppPayloadNormalizer(),
            (stationId, cardUid) ->
                new AuthorizationDecision(true, "Accepted", "DIRECT", null, null, "rfid_optional_disabled"));

    OcppSessionAuditService sessionAuditService =
        new OcppSessionAuditService(
            sessionRecordRepository, messageRecordRepository, objectMapper, noOpEvents);

    OcppSessionStateStore noOpStateStore =
        new OcppSessionStateStore(null) {
          @Override
          public void storePendingCommand(String stationId, String commandId, String action) {}

          @Override
          public void bindStationSession(String stationId, String sessionId) {}
        };
    OcppCommandDispatchService commandDispatchService =
        new OcppCommandDispatchService(
            commandRecordRepository,
            new OcppOutboundSessionRouter(frameCodec),
            noOpStateStore,
            new OcppCommandDispatchProperties(),
            objectMapper,
            noOpEvents);

    webSocketHandler =
        new OcppWebSocketHandler(
            frameCodec,
            bridgeService,
            sessionAuditService,
            commandDispatchService,
            new OcppOutboundSessionRouter(frameCodec),
            noOpStateStore);
  }

  @Test
  void replaysPythonCompatibleSequenceWithoutAckRegressions() throws Exception {
    CapturingWebSocketSession session = new CapturingWebSocketSession("python-suite-session");
    webSocketHandler.afterConnectionEstablished(session);

    replayMessage(
        session,
        new OcppMessage(
            "2",
            "boot-001",
            "BootNotification",
            Map.of(
                "stationId",
                "CP-PY-001",
                "chargePointVendor",
                "Arthexis",
                "chargePointModel",
                "Simulator")));

    replayMessage(
        session,
        new OcppMessage(
            "2",
            "status-001",
            "StatusNotification",
            Map.of(
                "stationId",
                "CP-PY-001",
                "status",
                "Available",
                "connectorId",
                1,
                "availability",
                "Operative")));

    replayMessage(
        session,
        new OcppMessage(
            "2",
            "txn-start-001",
            "TransactionEvent",
            Map.of(
                "stationId",
                "CP-PY-001",
                "eventType",
                "Started",
                "timestamp",
                "2026-03-31T10:00:00Z",
                "meterValue",
                List.of(
                    Map.of(
                        "sampledValue",
                        List.of(Map.of("measurand", "Power.Active.Import", "value", "9.5")))))));

    replayMessage(
        session,
        new OcppMessage(
            "2",
            "txn-end-001",
            "TransactionEvent",
            Map.of(
                "stationId",
                "CP-PY-001",
                "eventType",
                "Ended",
                "timestamp",
                "2026-03-31T10:15:00Z")));

    assertThat(session.acknowledgements).hasSize(4);
    assertAckAccepted(session.acknowledgements.get(0), "boot-001");
    assertAckAccepted(session.acknowledgements.get(1), "status-001");
    assertAckAccepted(session.acknowledgements.get(2), "txn-start-001");
    assertAckAccepted(session.acknowledgements.get(3), "txn-end-001");

    ChargingStation station = chargingStationRepository.findByStationId("CP-PY-001").orElseThrow();
    assertThat(station.getStatus()).isEqualTo("AVAILABLE");
    assertThat(station.getVendor()).isEqualTo("Arthexis");
    assertThat(station.getModel()).isEqualTo("Simulator");

    assertThat(telemetrySampleRepository.findAll())
        .anySatisfy(
            sample -> {
              assertThat(sample.getStationId()).isEqualTo("CP-PY-001");
              assertThat(sample.getMetricName()).isEqualTo("Power.Active.Import");
              assertThat(sample.getMetricValue()).isEqualTo(9.5d);
            });

    Integer messageCount =
        jdbcTemplate.queryForObject("select count(*) from ocpp_message_record", Integer.class);
    Integer acceptedOutboundCount =
        jdbcTemplate.queryForObject(
            """
            select count(*) from ocpp_message_record
            where direction = 'OUTBOUND' and result_status = 'Accepted'
            """,
            Integer.class);

    assertThat(messageCount).isEqualTo(8);
    assertThat(acceptedOutboundCount).isEqualTo(4);
  }

  private void replayMessage(WebSocketSession session, OcppMessage message) throws Exception {
    webSocketHandler.handleMessage(session, new TextMessage(frameCodec.encode(message)));
  }

  @SuppressWarnings("unchecked")
  private void assertAckAccepted(OcppMessage ack, String expectedMessageId) {
    assertThat(ack.messageType()).isEqualTo("CALLRESULT");
    assertThat(ack.messageId()).isEqualTo(expectedMessageId);
    assertThat(ack.action()).isNull();
    Map<String, Object> payload = (Map<String, Object>) ack.payload();
    assertThat(payload).containsEntry("status", "Accepted");
  }

  private class CapturingWebSocketSession implements WebSocketSession {

    private final String id;
    private final List<OcppMessage> acknowledgements = new ArrayList<>();

    private CapturingWebSocketSession(String id) {
      this.id = id;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public void sendMessage(WebSocketMessage<?> message) throws IOException {
      if (message instanceof TextMessage textMessage) {
        acknowledgements.add(frameCodec.decode(textMessage.getPayload()));
      } else {
        throw new IllegalArgumentException("Unexpected message type: " + message.getClass());
      }
    }

    @Override
    public URI getUri() {
      return null;
    }

    @Override
    public HttpHeaders getHandshakeHeaders() {
      return HttpHeaders.EMPTY;
    }

    @Override
    public Map<String, Object> getAttributes() {
      return Map.of();
    }

    @Override
    public java.security.Principal getPrincipal() {
      return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
      return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
      return null;
    }

    @Override
    public String getAcceptedProtocol() {
      return null;
    }

    @Override
    public void setTextMessageSizeLimit(int messageSizeLimit) {}

    @Override
    public int getTextMessageSizeLimit() {
      return 0;
    }

    @Override
    public void setBinaryMessageSizeLimit(int messageSizeLimit) {}

    @Override
    public int getBinaryMessageSizeLimit() {
      return 0;
    }

    @Override
    public List<WebSocketExtension> getExtensions() {
      return List.of();
    }

    @Override
    public boolean isOpen() {
      return true;
    }

    @Override
    public void close() {}

    @Override
    public void close(CloseStatus status) {}
  }
}
