package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OcaOcppBridgeServiceTests {

  private OcaOcppPayloadNormalizer normalizer;

  @BeforeEach
  void setUp() {
    normalizer = new OcaOcppPayloadNormalizer();
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
}
