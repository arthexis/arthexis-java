package com.arthexis.platform.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(TelemetryIngestionService.class)
class TelemetryIngestionServiceTests {

  @Autowired private TelemetryIngestionService telemetryIngestionService;

  @Autowired private TelemetrySampleRepository telemetrySampleRepository;

  @Test
  void ingestsNumericMeterValuesAsTelemetrySamples() {
    telemetryIngestionService.ingestMeterValues(
        "station-1",
        Map.of(
            "stationId", "station-1",
            "sampledAt", "2026-03-31T00:00:00Z",
            "powerKw", 18.5,
            "currentA", 32,
            "vendorStatus", "charging"));

    var samples = telemetrySampleRepository.findAll();

    assertThat(samples).hasSize(2);
    assertThat(samples)
        .extracting(TelemetrySample::getMetricName)
        .containsExactlyInAnyOrder("powerKw", "currentA");
    assertThat(samples)
        .allSatisfy(
            sample -> {
              assertThat(sample.getStationId()).isEqualTo("station-1");
              assertThat(sample.getScopeType()).isNull();
            });
  }

  @Test
  void ingestsStructuredSamplesWithDimensionsWhileKeepingLegacyFields() {
    telemetryIngestionService.ingestMeterValues(
        "station-1",
        Map.of(
            "sampledAt", "2026-03-31T00:00:00Z",
            "totalCost", 21.4,
            TelemetryIngestionService.STRUCTURED_SAMPLES_KEY,
                List.of(
                    Map.of(
                        "metricName", "Power.Active.Import",
                        "metricValue", 18.5,
                        "sampledAt", "2026-03-31T00:00:01Z",
                        "scopeType", "connector",
                        "scopeIdentifier", "evse-2:connector-1",
                        "unit", "W",
                        "phase", "L1",
                        "location", "Outlet",
                        "context", "Sample.Clock"))));

    var samples = telemetrySampleRepository.findAll();

    assertThat(samples).hasSize(2);
    assertThat(samples)
        .extracting(TelemetrySample::getMetricName)
        .containsExactlyInAnyOrder("Power.Active.Import", "totalCost");

    assertThat(samples)
        .filteredOn(sample -> sample.getMetricName().equals("Power.Active.Import"))
        .singleElement()
        .satisfies(
            sample -> {
              assertThat(sample.getStationId()).isEqualTo("station-1");
              assertThat(sample.getMetricValue()).isEqualTo(18.5d);
              assertThat(sample.getScopeType()).isEqualTo("connector");
              assertThat(sample.getScopeIdentifier()).isEqualTo("evse-2:connector-1");
              assertThat(sample.getUnit()).isEqualTo("W");
              assertThat(sample.getPhase()).isEqualTo("L1");
              assertThat(sample.getLocation()).isEqualTo("Outlet");
              assertThat(sample.getContext()).isEqualTo("Sample.Clock");
            });

    assertThat(samples)
        .filteredOn(sample -> sample.getMetricName().equals("totalCost"))
        .singleElement()
        .satisfies(
            sample -> {
              assertThat(sample.getStationId()).isEqualTo("station-1");
              assertThat(sample.getMetricValue()).isEqualTo(21.4d);
              assertThat(sample.getScopeType()).isNull();
              assertThat(sample.getScopeIdentifier()).isNull();
            });
  }

}
