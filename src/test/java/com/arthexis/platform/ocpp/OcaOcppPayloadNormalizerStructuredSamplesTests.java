package com.arthexis.platform.ocpp;

import static org.assertj.core.api.Assertions.assertThat;

import com.arthexis.platform.telemetry.TelemetryIngestionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OcaOcppPayloadNormalizerStructuredSamplesTests {

  private final OcaOcppPayloadNormalizer normalizer = new OcaOcppPayloadNormalizer();

  @Test
  void emitsStructuredSamplesWithScopeAndDimensions() {
    Map<String, Object> normalized =
        normalizer.normalizeMeterValues(
            "station-1",
            Map.of(
                "evseId", 2,
                "connectorId", 1,
                "meterValue",
                    List.of(
                        Map.of(
                            "timestamp", "2026-03-31T00:00:00Z",
                            "sampledValue",
                                List.of(
                                    Map.of(
                                        "measurand", "Power.Active.Import",
                                        "value", "7000",
                                        "unit", "W",
                                        "phase", "L1",
                                        "location", "Outlet",
                                        "context", "Sample.Periodic"))))));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> structuredSamples =
        (List<Map<String, Object>>) normalized.get(TelemetryIngestionService.STRUCTURED_SAMPLES_KEY);

    assertThat(structuredSamples).hasSize(1);
    assertThat(structuredSamples.getFirst())
        .containsEntry("metricName", "Power.Active.Import")
        .containsEntry("metricValue", 7000.0d)
        .containsEntry("scopeType", "connector")
        .containsEntry("scopeIdentifier", "evse-2:connector-1")
        .containsEntry("unit", "W")
        .containsEntry("phase", "L1")
        .containsEntry("location", "Outlet")
        .containsEntry("context", "Sample.Periodic")
        .containsEntry("sampledAt", "2026-03-31T00:00:00Z");

    assertThat(normalized).containsEntry("Power.Active.Import", 7000.0d);
  }

  @Test
  void includesEvseInConnectorScopeIdentifierToAvoidCrossEvseCollisions() {
    Map<String, Object> normalized =
        normalizer.normalizeMeterValues(
            "station-1",
            Map.of(
                "meterValue",
                    List.of(
                        Map.of(
                            "evse", Map.of("id", 1, "connectorId", 1),
                            "sampledValue",
                                List.of(Map.of("measurand", "Energy.Active.Import.Register", "value", "10"))),
                        Map.of(
                            "evse", Map.of("id", 2, "connectorId", 1),
                            "sampledValue",
                                List.of(Map.of("measurand", "Energy.Active.Import.Register", "value", "11"))))));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> structuredSamples =
        (List<Map<String, Object>>) normalized.get(TelemetryIngestionService.STRUCTURED_SAMPLES_KEY);

    assertThat(structuredSamples)
        .extracting(sample -> sample.get("scopeIdentifier"))
        .containsExactly("evse-1:connector-1", "evse-2:connector-1");
  }
}
