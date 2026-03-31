package com.arthexis.platform.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

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
        .allSatisfy(sample -> assertThat(sample.getStationId()).isEqualTo("station-1"));
  }
}
