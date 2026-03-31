package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.arthexis.platform.charging.ChargingConnectorState;
import com.arthexis.platform.charging.ChargingConnectorStateRepository;
import com.arthexis.platform.charging.ChargingConnectorStateService;
import com.arthexis.platform.charging.ChargingStation;
import com.arthexis.platform.charging.ChargingStationRepository;
import com.arthexis.platform.charging.ChargingStationService;
import com.arthexis.platform.telemetry.TelemetryIngestionService;
import com.arthexis.platform.telemetry.TelemetrySampleRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class OcaOcppBridgeStatusNotificationTests {

  @Mock private ChargingStationRepository chargingStationRepository;
  @Mock private ChargingConnectorStateRepository connectorStateRepository;
  @Mock private TelemetrySampleRepository telemetrySampleRepository;

  private OcaOcppBridgeService bridgeService;

  @BeforeEach
  void setUp() {
    bridgeService =
        new OcaOcppBridgeService(
            new ChargingStationService(chargingStationRepository),
            new ChargingConnectorStateService(connectorStateRepository),
            new OcppSessionStateStore(new StringRedisTemplate()),
            new TelemetryIngestionService(telemetrySampleRepository),
            new OcaOcppPayloadNormalizer());
  }

  @Test
  void mapsStatusNotificationToConnectorStateAndStationAggregateStatus() {
    when(connectorStateRepository.findByStationIdAndEvseIdAndConnectorId("CP-16", 1, 2))
        .thenReturn(Optional.empty());
    when(connectorStateRepository.save(any(ChargingConnectorState.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(connectorStateRepository.findByStationIdOrderByEvseIdAscConnectorIdAsc("CP-16"))
        .thenReturn(List.of(new ChargingConnectorState("CP-16", 1, 2, "CHARGING", "CCS2", "OPERATIVE", null)));

    when(chargingStationRepository.findByStationId("CP-16")).thenReturn(Optional.empty());
    when(chargingStationRepository.save(any(ChargingStation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OcppMessage incoming =
        new OcppMessage(
            "2",
            "msg-1",
            "StatusNotification",
            Map.of(
                "stationId", "CP-16",
                "status", "Charging",
                "connectorId", 2,
                "connectorType", "CCS2",
                "availability", "Operative"));

    Map<String, Object> response = bridgeService.handleIncoming("session-1", incoming);

    assertThat(response).containsEntry("status", "Accepted");

    ArgumentCaptor<ChargingStation> stationCaptor = ArgumentCaptor.forClass(ChargingStation.class);
    verify(chargingStationRepository).save(stationCaptor.capture());
    assertThat(stationCaptor.getValue().getStationId()).isEqualTo("CP-16");
    assertThat(stationCaptor.getValue().getStatus()).isEqualTo("CHARGING");

    ArgumentCaptor<ChargingConnectorState> connectorCaptor =
        ArgumentCaptor.forClass(ChargingConnectorState.class);
    verify(connectorStateRepository).save(connectorCaptor.capture());
    assertThat(connectorCaptor.getValue().getEvseId()).isEqualTo(1);
    assertThat(connectorCaptor.getValue().getConnectorId()).isEqualTo(2);
    assertThat(connectorCaptor.getValue().getConnectorStatus()).isEqualTo("CHARGING");
  }

  @Test
  void readsOcpp2xEvseBlockForConnectorIdentifiers() {
    when(connectorStateRepository.findByStationIdAndEvseIdAndConnectorId("CP-2X", 4, 7))
        .thenReturn(Optional.empty());
    when(connectorStateRepository.save(any(ChargingConnectorState.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(connectorStateRepository.findByStationIdOrderByEvseIdAscConnectorIdAsc("CP-2X"))
        .thenReturn(List.of(new ChargingConnectorState("CP-2X", 4, 7, "AVAILABLE", null, "OPERATIVE", null)));

    when(chargingStationRepository.findByStationId("CP-2X")).thenReturn(Optional.empty());
    when(chargingStationRepository.save(any(ChargingStation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    OcppMessage incoming =
        new OcppMessage(
            "2",
            "msg-2",
            "StatusNotification",
            Map.of(
                "chargingStation", Map.of("serialNumber", "CP-2X"),
                "connectorStatus", "Available",
                "evse", Map.of("id", 4, "connectorId", 7),
                "connectorAvailability", "Operative"));

    bridgeService.handleIncoming("session-2", incoming);

    ArgumentCaptor<ChargingConnectorState> connectorCaptor =
        ArgumentCaptor.forClass(ChargingConnectorState.class);
    verify(connectorStateRepository).save(connectorCaptor.capture());
    assertThat(connectorCaptor.getValue().getStationId()).isEqualTo("CP-2X");
    assertThat(connectorCaptor.getValue().getEvseId()).isEqualTo(4);
    assertThat(connectorCaptor.getValue().getConnectorId()).isEqualTo(7);
  }
}
