package com.arthexis.platform.operations;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "arthexis.runtime")
public class RuntimeBudgetProperties {

  private Duration startupBudget = Duration.ofSeconds(90);
  private Duration migrationBudget = Duration.ofSeconds(30);

  public Duration getStartupBudget() {
    return startupBudget;
  }

  public void setStartupBudget(Duration startupBudget) {
    this.startupBudget = startupBudget;
  }

  public Duration getMigrationBudget() {
    return migrationBudget;
  }

  public void setMigrationBudget(Duration migrationBudget) {
    this.migrationBudget = migrationBudget;
  }
}
