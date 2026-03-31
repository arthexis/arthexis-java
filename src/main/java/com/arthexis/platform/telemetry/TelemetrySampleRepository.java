package com.arthexis.platform.telemetry;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelemetrySampleRepository extends JpaRepository<TelemetrySample, Long> {

  List<TelemetrySample> findTop120ByStationIdAndMetricNameOrderBySampledAtDesc(
      String stationId, String metricName);

  List<TelemetrySample> findTop100ByStationIdOrderBySampledAtDesc(String stationId);
}
