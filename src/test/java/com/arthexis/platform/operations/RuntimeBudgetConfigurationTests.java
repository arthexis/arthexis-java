package com.arthexis.platform.operations;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

class RuntimeBudgetConfigurationTests {

  @Test
  void migrationStrategyRunsFlywayAndRecordsMetrics() {
    var meterRegistry = new SimpleMeterRegistry();
    var recorder = new RuntimeBudgetRecorder(meterRegistry);
    var properties = new RuntimeBudgetProperties();
    properties.setMigrationBudget(Duration.ofMillis(1));

    FlywayMigrationStrategy strategy =
        new RuntimeBudgetConfiguration().migrationBudgetStrategy(recorder, properties);

    Flyway flyway =
        Flyway.configure()
            .dataSource("jdbc:h2:mem:runtime_budget_config;DB_CLOSE_DELAY=-1", "sa", "")
            .locations("classpath:db/migration")
            .load();

    strategy.migrate(flyway);

    assertThat(meterRegistry.get("arthexis.runtime.phase.duration").tag("phase", "migration").timer())
        .isNotNull();
  }
}
