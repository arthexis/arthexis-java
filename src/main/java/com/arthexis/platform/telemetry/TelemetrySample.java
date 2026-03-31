package com.arthexis.platform.telemetry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "telemetry_sample")
public class TelemetrySample {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "station_id", nullable = false, length = 64)
  private String stationId;

  @Column(name = "metric_name", nullable = false, length = 64)
  private String metricName;

  @Column(name = "metric_value", nullable = false)
  private double metricValue;

  @Column(name = "scope_type", length = 32)
  private String scopeType;

  @Column(name = "scope_identifier", length = 128)
  private String scopeIdentifier;

  @Column(name = "unit", length = 32)
  private String unit;

  @Column(name = "phase", length = 32)
  private String phase;

  @Column(name = "location", length = 32)
  private String location;

  @Column(name = "context", length = 64)
  private String context;

  @Column(name = "sampled_at", nullable = false)
  private Instant sampledAt;

  protected TelemetrySample() {}

  public TelemetrySample(String stationId, String metricName, double metricValue, Instant sampledAt) {
    this(stationId, metricName, metricValue, null, null, null, null, null, null, sampledAt);
  }

  public TelemetrySample(
      String stationId,
      String metricName,
      double metricValue,
      String scopeType,
      String scopeIdentifier,
      String unit,
      String phase,
      String location,
      String context,
      Instant sampledAt) {
    this.stationId = stationId;
    this.metricName = metricName;
    this.metricValue = metricValue;
    this.scopeType = scopeType;
    this.scopeIdentifier = scopeIdentifier;
    this.unit = unit;
    this.phase = phase;
    this.location = location;
    this.context = context;
    this.sampledAt = sampledAt;
  }

  public Long getId() {
    return id;
  }

  public String getStationId() {
    return stationId;
  }

  public String getMetricName() {
    return metricName;
  }

  public double getMetricValue() {
    return metricValue;
  }

  public String getScopeType() {
    return scopeType;
  }

  public String getScopeIdentifier() {
    return scopeIdentifier;
  }

  public String getUnit() {
    return unit;
  }

  public String getPhase() {
    return phase;
  }

  public String getLocation() {
    return location;
  }

  public String getContext() {
    return context;
  }

  public Instant getSampledAt() {
    return sampledAt;
  }
}
