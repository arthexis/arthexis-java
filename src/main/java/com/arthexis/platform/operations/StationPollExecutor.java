package com.arthexis.platform.operations;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class StationPollExecutor {

  private final MeterRegistry meterRegistry;

  public StationPollExecutor(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void execute(String stationScope) {
    meterRegistry.counter("arthexis.jobs.station.poll.executed", "scope", normalize(stationScope)).increment();
  }

  private String normalize(String stationScope) {
    return stationScope == null || stationScope.isBlank() ? "all" : stationScope;
  }
}
