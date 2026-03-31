package com.arthexis.platform.operations;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RuntimeBudgetRecorder {

  private static final Logger logger = LoggerFactory.getLogger(RuntimeBudgetRecorder.class);

  private final MeterRegistry meterRegistry;

  public RuntimeBudgetRecorder(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  public void record(String phase, Duration actualDuration, Duration budget) {
    Timer.builder("arthexis.runtime.phase.duration")
        .description("Startup and migration durations by phase")
        .tag("phase", phase)
        .register(meterRegistry)
        .record(actualDuration);

    if (actualDuration.compareTo(budget) > 0) {
      Counter.builder("arthexis.runtime.phase.over_budget.total")
          .description("Number of times runtime phases exceeded budget")
          .tag("phase", phase)
          .register(meterRegistry)
          .increment();
      logger.warn(
          "Runtime phase '{}' exceeded budget (actual={}ms, budget={}ms). "
              + "This does not stop the live instance; investigate and tune rollout plans.",
          phase,
          actualDuration.toMillis(),
          budget.toMillis());
      return;
    }

    logger.info(
        "Runtime phase '{}' completed within budget (actual={}ms, budget={}ms).",
        phase,
        actualDuration.toMillis(),
        budget.toMillis());
  }
}
