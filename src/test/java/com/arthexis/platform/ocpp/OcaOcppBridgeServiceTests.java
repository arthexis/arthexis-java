package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arthexis.platform.auth.AuthorizationDecision;
import com.arthexis.platform.auth.RfidAuthorizationGateway;
import com.arthexis.platform.charging.ChargingConnectorStateService;
import com.arthexis.platform.charging.ChargingStationService;
import com.arthexis.platform.telemetry.TelemetryIngestionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OcaOcppBridgeServiceTests {

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
  void startTransactionLegacyActionUpdatesConnectorAndStationState() {
    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-start",
            new OcppMessage(
                "2",
                "msg-start-16",
                "StartTransaction",
                Map.of(
                    "stationId",
                    "CP-16",
                    "connectorId",
                    2,
                    "idTag",
                    "CARD-16",
                    "meterStart",
                    100,
                    "transactionId",
                    55)));

    verify(connectorStateService)
        .upsertConnectorState(eq("CP-16"), eq(1), eq(2), eq("CHARGING"), eq(""), eq("Operative"), any());
    verify(chargingStationService).upsertStatus(eq("CP-16"), eq("CHARGING"), any());
    assertThat(response.payload())
        .isEqualTo(Map.of("idTagInfo", Map.of("status", "Accepted"), "transactionId", 55));
  }

  @Test
  void stopTransactionLegacyActionUpdatesConnectorAndStationState() {
    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-stop",
            new OcppMessage(
                "2",
                "msg-stop-16",
                "StopTransaction",
                Map.of("stationId", "CP-16", "connectorId", 2, "meterStop", 125, "transactionId", 55)));

    verify(connectorStateService)
        .upsertConnectorState(eq("CP-16"), eq(1), eq(2), eq("AVAILABLE"), eq(""), eq("Operative"), any());
    verify(chargingStationService).upsertStatus(eq("CP-16"), eq("AVAILABLE"), any());
    assertThat(response.payload()).isEqualTo(Map.of("idTagInfo", Map.of("status", "Accepted")));
  }

  @Test
  void changeConfigurationStoresPendingCommandAndReturnsAccepted() {
    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-cfg",
            new OcppMessage(
                "2",
                "msg-change-cfg",
                "ChangeConfiguration",
                Map.of("stationId", "CP-16", "key", "HeartbeatInterval", "value", "120")));

    verify(chargingStationService).upsertStatus(eq("CP-16"), eq("ONLINE"), any());
    verify(stateStore).storePendingCommand("CP-16", "msg-change-cfg", "ChangeConfiguration");
    assertThat(response.payload())
        .isEqualTo(Map.of("status", "Accepted", "key", "HeartbeatInterval"));
  }

  @Test
  void getConfigurationReturnsCompatibilityShape() {
    OcppBridgeResponse response =
        bridgeService.handleIncoming(
            "session-cfg",
            new OcppMessage(
                "2",
                "msg-get-cfg",
                "GetConfiguration",
                Map.of("stationId", "CP-16", "key", List.of("HeartbeatInterval"))));

    verify(chargingStationService).upsertStatus(eq("CP-16"), eq("ONLINE"), any());
    assertThat(response.payload())
        .isEqualTo(
            Map.of(
                "configurationKey",
                List.of(Map.of("key", "HeartbeatInterval", "readonly", false)),
                "unknownKey",
                List.of()));
  }
}
