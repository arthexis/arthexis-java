package com.arthexis.platform.operations;

import java.time.Duration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupBudgetMonitor {

  private final RuntimeBudgetRecorder runtimeBudgetRecorder;
  private final RuntimeBudgetProperties runtimeBudgetProperties;

  public StartupBudgetMonitor(
      RuntimeBudgetRecorder runtimeBudgetRecorder, RuntimeBudgetProperties runtimeBudgetProperties) {
    this.runtimeBudgetRecorder = runtimeBudgetRecorder;
    this.runtimeBudgetProperties = runtimeBudgetProperties;
  }

  @EventListener
  public void onApplicationReady(ApplicationReadyEvent event) {
    Duration startupDuration = event.getTimeTaken() != null ? event.getTimeTaken() : Duration.ZERO;
    runtimeBudgetRecorder.record(
        "startup", startupDuration, runtimeBudgetProperties.getStartupBudget());
  }
}
