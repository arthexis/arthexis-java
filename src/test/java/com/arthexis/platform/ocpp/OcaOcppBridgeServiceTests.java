package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arthexis.platform.auth.AuthorizationDecision;
import com.arthexis.platform.auth.RfidAuthorizationGateway;
import com.arthexis.platform.charging.ChargingConnectorStateService;
import com.arthexis.platform.charging.ChargingStationService;
import com.arthexis.platform.telemetry.TelemetryIngestionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OcaOcppBridgeServiceTests {
  private static final Path FIXTURE_DIR = Path.of("docs/ocpp/fixtures");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Mock private ChargingStationService chargingStationService;
  @Mock private ChargingConnectorStateService connectorStateService;
  @Mock private OcppSessionStateStore stateStore;
  @Mock private TelemetryIngestionService telemetryIngestionService;
  @Mock private RfidAuthorizationGateway rfidAuthorizationGateway;

  private OcaOcppPayloadNormalizer normalizer;
  private OcaOcppBridgeService bridgeService;

  @BeforeEach
  void setUp() {
    normalizer = new OcaOcppPayloadNormalizer();
    bridgeService =
        new OcaOcppBridgeService(
            chargingStationService,
            connectorStateService,
            stateStore,
            telemetryIngestionService,
            normalizer,
            rfidAuthorizationGateway);
  }

  @Test
  void resolvesOcpp16StationIdentifierFromStationId() {
    String stationId = normalizer.resolveStationId("session-1", Map.of("stationId", "CP-16"));

    assertThat(stationId).isEqualTo("CP-16");
  }

  @Test
  void flattensOcpp16MeterValuesIntoTelemetryMetrics() {
    Map<String, Object> normalizedPayload =
        normalizer.normalizeMeterValues(
            "CP-16",
            Map.of(
                "meterValue",
                List.of(
                    Map.of(
                        "timestamp",
                        "2026-03-31T00:00:00Z",
                        "sampledValue",
                        List.of(
                            Map.of("measurand", "Power.Active.Import", "value", "7.50"),
                            Map.of("measurand", "Current.Import", "value", 32))))));

    assertThat(normalizedPayload).containsEntry("stationId", "CP-16");
    assertThat(normalizedPayload).containsEntry("Power.Active.Import", 7.5d);
    assertThat(normalizedPayload).containsEntry("Current.Import", 32d);
    assertThat(normalizedPayload).containsEntry("sampledAt", "2026-03-31T00:00:00Z");
  }

  @Test
  void supportsOcpp2xTransactionEventUsingChargingStationBlock() {
    String stationId =
        normalizer.resolveStationId(
            "session-3", Map.of("chargingStation", Map.of("serialNumber", "CP-2X", "model", "AX-50")));

    Map<String, Object> normalizedPayload =
        normalizer.normalizeTransactionEvent(
            stationId,
            Map.of(
                "eventType",
                "Started",
                "timestamp",
                "2026-03-31T01:00:00Z",
                "meterValue",
                List.of(
                    Map.of(
                        "sampledValue",
                        List.of(Map.of("measurand", "Energy.Active.Import.Register", "value", "12.3"))))));

    assertThat(stationId).isEqualTo("CP-2X");
    assertThat(normalizedPayload).containsEntry("stationId", "CP-2X");
    assertThat(normalizedPayload).containsEntry("Energy.Active.Import.Register", 12.3d);
    assertThat(normalizedPayload).containsEntry("sampledAt", "2026-03-31T01:00:00Z");
  }

  @Test
  void authorizeOcpp16UsesIdTagAndReturnsAcceptedStatus() {
    when(rfidAuthorizationGateway.authorize("CP-16", "CARD-16"))
        .thenReturn(new AuthorizationDecision(true, "Accepted", "RFID", "acct-16", null, "ok"));

    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-16",
            new OcppMessage(
                "2", "msg-auth-16", "Authorize", Map.of("stationId", "CP-16", "idTag", "CARD-16")));

    assertThat(response.payload()).isEqualTo(Map.of("idTagInfo", Map.of("status", "Accepted")));
  }

  @Test
  void authorizeOcpp16UsesIdTagAndReturnsDeniedStatus() {
    when(rfidAuthorizationGateway.authorize("CP-16", "CARD-16"))
        .thenReturn(
            new AuthorizationDecision(
                false, "Blocked", "ACCOUNT_LOGIN", "acct-16", "https://example/login", "blocked"));

    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-16",
            new OcppMessage(
                "2", "msg-auth-16", "Authorize", Map.of("stationId", "CP-16", "idTag", "CARD-16")));

    assertThat(response.payload()).isEqualTo(Map.of("idTagInfo", Map.of("status", "Blocked")));
  }

  @Test
  void authorizeOcpp2xUsesIdTokenAndReturnsAcceptedStatus() {
    when(rfidAuthorizationGateway.authorize("CP-2X", "CARD-2X"))
        .thenReturn(new AuthorizationDecision(true, "Accepted", "RFID", "acct-2x", null, "ok"));

    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-2x",
            new OcppMessage(
                "2",
                "msg-auth-2x",
                "Authorize",
                Map.of(
                    "chargingStation", Map.of("serialNumber", "CP-2X"),
                    "idToken", Map.of("idToken", "CARD-2X"))));

    assertThat(response.payload())
        .containsEntry(
            "idTokenInfo",
            Map.of(
                "status",
                "Accepted",
                "customData",
                Map.of("authMode", "RFID", "reason", "ok")));
    assertThat(response.payload()).containsEntry("customData", Map.of("accountExternalId", "acct-2x", "cardUid", "CARD-2X"));
  }

  @Test
  void authorizeOcpp2xUsesIdTokenAndReturnsDeniedStatus() {
    when(rfidAuthorizationGateway.authorize("CP-2X", "CARD-2X"))
        .thenReturn(
            new AuthorizationDecision(
                false,
                "Invalid",
                "ACCOUNT_LOGIN",
                "acct-2x",
                "https://example/login",
                "rfid_card_unknown"));

    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-2x",
            new OcppMessage(
                "2",
                "msg-auth-2x",
                "Authorize",
                Map.of(
                    "chargingStation", Map.of("serialNumber", "CP-2X"),
                    "idToken", Map.of("idToken", "CARD-2X"))));

    assertThat(response.payload())
        .containsEntry(
            "idTokenInfo",
            Map.of(
                "status",
                "Invalid",
                "customData",
                Map.of("authMode", "ACCOUNT_LOGIN", "reason", "rfid_card_unknown")));
    assertThat(response.payload())
        .containsEntry(
            "customData",
            Map.of(
                "accountExternalId",
                "acct-2x",
                "cardUid",
                "CARD-2X",
                "loginUrl",
                "https://example/login"));
  }

  @Test
  void authorizeMissingTokenReturnsInvalidStatus() {
    when(rfidAuthorizationGateway.authorize("CP-16", null))
        .thenReturn(
            new AuthorizationDecision(
                false, "Invalid", "ACCOUNT_LOGIN", null, null, "rfid_card_unknown"));

    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-missing",
            new OcppMessage("2", "msg-auth-missing", "Authorize", Map.of("stationId", "CP-16")));

    assertThat(response.payload()).isEqualTo(Map.of("idTagInfo", Map.of("status", "Invalid")));
  }

  @Test
  void startTransactionCompatibilityMarksStationChargingAndReturnsTransactionId() {
    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-16",
            new OcppMessage(
                "2",
                "msg-start-16",
                "StartTransaction",
                Map.of("stationId", "CP-16", "idTag", "CARD-16", "connectorId", 1)));

    verify(chargingStationService).upsertStatus("CP-16", "CHARGING");
    assertThat(response.payload()).containsEntry("idTagInfo", Map.of("status", "Accepted"));
    assertThat(response.payload()).containsKey("transactionId");
  }

  @Test
  void stopTransactionCompatibilityMarksStationAvailable() {
    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-16",
            new OcppMessage(
                "2",
                "msg-stop-16",
                "StopTransaction",
                Map.of("stationId", "CP-16", "transactionId", 44)));

    verify(chargingStationService).upsertStatus("CP-16", "AVAILABLE");
    assertThat(response.payload()).isEqualTo(Map.of("status", "Accepted"));
  }

  @Test
  void securityEventNotificationReturnsExplicitNoOpPayloadAndAuditStatus() {
    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-2x",
            new OcppMessage(
                "2",
                "msg-sec-2x",
                "SecurityEventNotification",
                Map.of("chargingStation", Map.of("serialNumber", "CP-2X"), "type", "Tamper")));

    verify(chargingStationService).upsertStatus(any(), any(), any());
    assertThat(response.resultStatus()).isEqualTo("unsupported-but-accepted");
    assertThat(response.payload())
        .containsEntry("status", "Accepted")
        .containsEntry("customData", Map.of("handling", "no-op", "auditStatus", "unsupported-but-accepted"));
  }

  @Test
  void unknownActionReturnsUnsupportedButAcceptedForAuditing() {
    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-any",
            new OcppMessage("2", "msg-unknown", "GetConfiguration", Map.of("stationId", "CP-16")));

    assertThat(response.resultStatus()).isEqualTo("unsupported-but-accepted");
    assertThat(response.payload())
        .containsEntry("status", "Accepted")
        .containsEntry("customData", Map.of("handling", "no-op", "auditStatus", "unsupported-but-accepted"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("bridgeCompatibilityFixtures")
  void keepsBridgeCompatibilityContractForParityActions(String fixtureFile) throws IOException {
    Map<String, Object> fixture = readFixture(fixtureFile);
    Map<String, Object> payload = mapValue(fixture.get("payload"));
    Map<String, Object> expected = mapValue(fixture.get("expected"));

    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            stringValue(fixture.get("sessionId")),
            new OcppMessage("2", "fixture-msg", stringValue(fixture.get("action")), payload));

    assertThat(response.stationId()).isEqualTo(stringValue(expected.get("resolvedStationId")));
    Map<String, Object> expectedPayload = mapValue(expected.get("responsePayload"));
    expectedPayload.forEach((key, value) -> assertThat(response.payload()).containsEntry(key, value));
    if (expected.containsKey("resultStatus")) {
      assertThat(response.resultStatus()).isEqualTo(stringValue(expected.get("resultStatus")));
    }
  }

  private static Stream<Arguments> bridgeCompatibilityFixtures() {
    return Stream.of(
            "start_transaction.ocpp16.json",
            "stop_transaction.ocpp16.json",
            "security_event_notification.ocpp2x.json",
            "notify_event.ocpp2x.json")
        .map(Arguments::of);
  }

  private static Map<String, Object> readFixture(String fixtureFile) throws IOException {
    String json = Files.readString(FIXTURE_DIR.resolve(fixtureFile));
    return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
  }

  private static String stringValue(Object value) {
    return value == null ? "" : value.toString();
  }

  private static Map<String, Object> mapValue(Object value) {
    if (value instanceof Map<?, ?> map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> typed = (Map<String, Object>) map;
      return typed;
    }
    return Map.of();
  }
}
