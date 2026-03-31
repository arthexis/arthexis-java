package com.arthexis.platform.operations;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RuntimeBudgetRecorderTests {

  @Test
  void incrementsOverBudgetCounterWhenDurationExceedsBudget() {
    var meterRegistry = new SimpleMeterRegistry();
    var recorder = new RuntimeBudgetRecorder(meterRegistry);

    recorder.record("startup", Duration.ofSeconds(5), Duration.ofSeconds(1));

    assertThat(
            meterRegistry
                .get("arthexis.runtime.phase.over_budget.total")
                .tag("phase", "startup")
                .counter()
                .count())
        .isEqualTo(1.0);
  }

  @Test
  void doesNotIncrementOverBudgetCounterWhenDurationIsWithinBudget() {
    var meterRegistry = new SimpleMeterRegistry();
    var recorder = new RuntimeBudgetRecorder(meterRegistry);

    recorder.record("migration", Duration.ofSeconds(3), Duration.ofSeconds(3));

    assertThat(
            meterRegistry
                .find("arthexis.runtime.phase.over_budget.total")
                .tag("phase", "migration")
                .counter())
        .isNull();
  }
}
