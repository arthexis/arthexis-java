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

  @Column(name = "sampled_at", nullable = false)
  private Instant sampledAt;

  protected TelemetrySample() {}

  public TelemetrySample(String stationId, String metricName, double metricValue, Instant sampledAt) {
    this.stationId = stationId;
    this.metricName = metricName;
    this.metricValue = metricValue;
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

  public Instant getSampledAt() {
    return sampledAt;
  }
}
