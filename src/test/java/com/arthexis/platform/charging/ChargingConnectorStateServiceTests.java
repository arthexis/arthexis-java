package com.arthexis.platform.charging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChargingConnectorStateServiceTests {

  @Mock private ChargingConnectorStateRepository repository;

  private ChargingConnectorStateService service;

  @BeforeEach
  void setUp() {
    service = new ChargingConnectorStateService(repository);
  }

  @Test
  void derivesStationStatusUsingDeterministicPrecedence() {
    when(repository.findByStationIdOrderByEvseIdAscConnectorIdAsc("CP-1"))
        .thenReturn(
            List.of(
                new ChargingConnectorState("CP-1", 1, 1, "AVAILABLE", "TYPE2", "OPERATIVE", null),
                new ChargingConnectorState("CP-1", 1, 2, "CHARGING", "CCS2", "OPERATIVE", null),
                new ChargingConnectorState("CP-1", 2, 1, "FAULTED", "CCS2", "INOPERATIVE", null)));

    String aggregate = service.deriveStationAggregateStatus("CP-1", "ONLINE");

    assertThat(aggregate).isEqualTo("FAULTED");
  }

  @Test
  void usesFallbackWhenNoConnectorStateExists() {
    when(repository.findByStationIdOrderByEvseIdAscConnectorIdAsc("CP-2")).thenReturn(List.of());

    String aggregate = service.deriveStationAggregateStatus("CP-2", "available");

    assertThat(aggregate).isEqualTo("AVAILABLE");
  }

  @Test
  void upsertNormalizesConnectorFields() {
    when(repository.findByStationIdAndEvseIdAndConnectorId("CP-3", 1, 3)).thenReturn(Optional.empty());
    when(repository.save(any(ChargingConnectorState.class))).thenAnswer(invocation -> invocation.getArgument(0));

    ChargingConnectorState connectorState =
        service.upsertConnectorState("CP-3", 1, 3, "SuspendedEvse", "iec 62196-2", "operative", null);

    assertThat(connectorState.getConnectorStatus()).isEqualTo("SUSPENDED_EVSE");
    assertThat(connectorState.getConnectorType()).isEqualTo("IEC_62196_2");
    assertThat(connectorState.getAvailability()).isEqualTo("OPERATIVE");
    assertThat(connectorState.getLastStatusAt()).isNotNull();
  }
}
