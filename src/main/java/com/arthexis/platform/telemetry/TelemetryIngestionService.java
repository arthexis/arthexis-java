package com.arthexis.platform.telemetry;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryIngestionService {

  private final TelemetrySampleRepository repository;

  public TelemetryIngestionService(TelemetrySampleRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void ingestMeterValues(String stationId, Map<String, Object> payload) {
    Instant sampledAt = extractSampledAt(payload);

    payload.entrySet().stream()
        .filter(entry -> entry.getValue() instanceof Number)
        .filter(entry -> !"stationId".equals(entry.getKey()))
        .forEach(
            entry ->
                repository.save(
                    new TelemetrySample(
                        stationId,
                        entry.getKey(),
                        ((Number) entry.getValue()).doubleValue(),
                        sampledAt)));
  }

  private Instant extractSampledAt(Map<String, Object> payload) {
    Object rawSampledAt = payload.get("sampledAt");
    if (rawSampledAt instanceof String sampledAtText) {
      try {
        return Instant.parse(sampledAtText);
      } catch (DateTimeParseException ignored) {
        return Instant.now();
      }
    }
    return Instant.now();
  }
}
