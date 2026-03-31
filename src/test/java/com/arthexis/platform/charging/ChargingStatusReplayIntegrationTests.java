package com.arthexis.platform.charging;

import static org.assertj.core.api.Assertions.assertThat;

import com.arthexis.platform.ocpp.OcaOcppBridgeService;
import com.arthexis.platform.ocpp.OcaOcppPayloadNormalizer;
import com.arthexis.platform.ocpp.OcppBridgeResponse;
import com.arthexis.platform.ocpp.OcppMessage;
import com.arthexis.platform.ocpp.OcppSessionStateStore;
import com.arthexis.platform.telemetry.TelemetryIngestionService;
import com.arthexis.platform.telemetry.TelemetrySampleRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;

@DataJpaTest
class ChargingStatusReplayIntegrationTests {

  @Autowired private ChargingStationRepository chargingStationRepository;
  @Autowired private ChargingConnectorStateRepository connectorStateRepository;
  @Autowired private TelemetrySampleRepository telemetrySampleRepository;

  private OcaOcppBridgeService bridgeService;

  @BeforeEach
  void setUp() {
    telemetrySampleRepository.deleteAll();
    connectorStateRepository.deleteAll();
    chargingStationRepository.deleteAll();

    ApplicationEventPublisher noOpEvents = event -> {};
    bridgeService =
        new OcaOcppBridgeService(
            new ChargingStationService(chargingStationRepository, noOpEvents),
            new ChargingConnectorStateService(connectorStateRepository),
            new OcppSessionStateStore(null) {
              @Override
              public void storePendingCommand(String stationId, String commandId, String action) {}
            },
            new TelemetryIngestionService(telemetrySampleRepository, noOpEvents),
            new OcaOcppPayloadNormalizer());
  }

  @Test
  void replaysStatusAndTransactionTransitionsWithStableAcceptedResponses() {
    String sessionId = "charging-sequence-session";

    OcppBridgeResponse bootAck =
        bridgeService.handleIncoming(
            sessionId,
            new OcppMessage(
                "2",
                "boot-2x",
                "BootNotification",
                Map.of(
                    "chargingStation",
                    Map.of("serialNumber", "CP-2X-001", "vendorName", "Arthexis", "model", "AX-50"))));

    OcppBridgeResponse statusAck =
        bridgeService.handleIncoming(
            sessionId,
            new OcppMessage(
                "2",
                "status-2x",
                "StatusNotification",
                Map.of(
                    "chargingStation",
                    Map.of("serialNumber", "CP-2X-001"),
                    "connectorStatus",
                    "Charging",
                    "evse",
                    Map.of("id", 2, "connectorId", 3),
                    "connectorType",
                    "CCS2",
                    "connectorAvailability",
                    "Operative")));

    OcppBridgeResponse transactionAck =
        bridgeService.handleIncoming(
            sessionId,
            new OcppMessage(
                "2",
                "tx-2x",
                "TransactionEvent",
                Map.of(
                    "chargingStation",
                    Map.of("serialNumber", "CP-2X-001"),
                    "eventType",
                    "Started",
                    "timestamp",
                    "2026-03-31T11:00:00Z",
                    "meterValues",
                    List.of(
                        Map.of(
                            "sampledValue",
                            List.of(
                                Map.of(
                                    "measurand",
                                    "Energy.Active.Import.Register",
                                    "value",
                                    "12.75")))))));

    assertThat(bootAck.resultStatus()).isEqualTo("Accepted");
    assertThat(statusAck.resultStatus()).isEqualTo("Accepted");
    assertThat(transactionAck.resultStatus()).isEqualTo("Accepted");

    ChargingStation station = chargingStationRepository.findByStationId("CP-2X-001").orElseThrow();
    assertThat(station.getStatus()).isEqualTo("CHARGING");
    assertThat(station.getVendor()).isEqualTo("Arthexis");
    assertThat(station.getModel()).isEqualTo("AX-50");

    ChargingConnectorState connector =
        connectorStateRepository.findByStationIdAndEvseIdAndConnectorId("CP-2X-001", 2, 3).orElseThrow();
    assertThat(connector.getConnectorStatus()).isEqualTo("CHARGING");
    assertThat(connector.getConnectorType()).isEqualTo("CCS2");
    assertThat(connector.getAvailability()).isEqualTo("OPERATIVE");

    assertThat(telemetrySampleRepository.findAll())
        .anySatisfy(
            sample -> {
              assertThat(sample.getStationId()).isEqualTo("CP-2X-001");
              assertThat(sample.getMetricName()).isEqualTo("Energy.Active.Import.Register");
              assertThat(sample.getMetricValue()).isEqualTo(12.75d);
            });
  }
}
