package com.arthexis.platform.telemetry;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TelemetryIngestionService {

  public static final String STRUCTURED_SAMPLES_KEY = "_structuredSamples";

  private final TelemetrySampleRepository repository;

  public TelemetryIngestionService(TelemetrySampleRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void ingestMeterValues(String stationId, Map<String, Object> payload) {
    Instant defaultSampledAt = extractSampledAt(payload);
    List<Map<String, Object>> structuredSamples = extractStructuredSamples(payload);

    structuredSamples.forEach(sample -> repository.save(buildSample(stationId, sample, defaultSampledAt)));

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
                        defaultSampledAt)));
  }

  private TelemetrySample buildSample(
      String stationId, Map<String, Object> structuredSample, Instant defaultSampledAt) {
    String metricName = stringValue(structuredSample.get("metricName"));
    Double metricValue = numericValue(structuredSample.get("metricValue"));
    if (metricName.isBlank() || metricValue == null) {
      throw new IllegalArgumentException("Structured telemetry samples must include metricName and metricValue");
    }

    return new TelemetrySample(
        stationId,
        metricName,
        metricValue,
        emptyToNull(stringValue(structuredSample.get("scopeType"))),
        emptyToNull(stringValue(structuredSample.get("scopeIdentifier"))),
        emptyToNull(stringValue(structuredSample.get("unit"))),
        emptyToNull(stringValue(structuredSample.get("phase"))),
        emptyToNull(stringValue(structuredSample.get("location"))),
        emptyToNull(stringValue(structuredSample.get("context"))),
        extractSampledAt(structuredSample, defaultSampledAt));
  }

  private List<Map<String, Object>> extractStructuredSamples(Map<String, Object> payload) {
    Object raw = payload.get(STRUCTURED_SAMPLES_KEY);
    if (!(raw instanceof List<?> list)) {
      return List.of();
    }

    return list.stream()
        .filter(Map.class::isInstance)
        .map(
            item -> {
              @SuppressWarnings("unchecked")
              Map<String, Object> map = (Map<String, Object>) item;
              return map;
            })
        .toList();
  }

  private Instant extractSampledAt(Map<String, Object> payload) {
    return extractSampledAt(payload, Instant.now());
  }

  private Instant extractSampledAt(Map<String, Object> payload, Instant defaultValue) {
    Object rawSampledAt = payload.get("sampledAt");
    if (rawSampledAt instanceof String sampledAtText) {
      try {
        return Instant.parse(sampledAtText);
      } catch (DateTimeParseException ignored) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  private Double numericValue(Object value) {
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    return null;
  }

  private String stringValue(Object value) {
    return value == null ? "" : value.toString();
  }

  private String emptyToNull(String value) {
    return value.isBlank() ? null : value;
  }
}
