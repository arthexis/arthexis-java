package com.arthexis.platform.operations;

import java.time.Duration;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RuntimeBudgetProperties.class)
public class RuntimeBudgetConfiguration {

  @Bean
  @ConditionalOnClass(Flyway.class)
  @ConditionalOnMissingBean(FlywayMigrationStrategy.class)
  FlywayMigrationStrategy migrationBudgetStrategy(
      RuntimeBudgetRecorder runtimeBudgetRecorder, RuntimeBudgetProperties runtimeBudgetProperties) {
    return flyway -> {
      long startedAt = System.nanoTime();
      flyway.migrate();
      Duration migrationDuration = Duration.ofNanos(System.nanoTime() - startedAt);
      runtimeBudgetRecorder.record(
          "migration", migrationDuration, runtimeBudgetProperties.getMigrationBudget());
    };
  }
}
