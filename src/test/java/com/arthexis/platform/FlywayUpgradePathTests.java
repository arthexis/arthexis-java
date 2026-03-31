package com.arthexis.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

class FlywayUpgradePathTests {

  @Test
  void migratesExistingInstallFromV1ToLatest() throws Exception {
    Path databaseFile = Files.createTempFile("arthexis-upgrade", ".db");
    String databaseUrl =
        "jdbc:h2:file:"
            + databaseFile.toAbsolutePath()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

    Flyway initialInstallMigrations =
        flywayFor(databaseUrl).target(MigrationVersion.fromVersion("1")).load();
    MigrateResult initialInstallResult = initialInstallMigrations.migrate();

    assertThat(initialInstallResult.success).isTrue();
    assertThat(initialInstallMigrations.info().current()).isNotNull();
    assertThat(initialInstallMigrations.info().current().getVersion().getVersion()).isEqualTo("1");

    Flyway upgradeMigrations = flywayFor(databaseUrl).load();
    MigrateResult upgradeResult = upgradeMigrations.migrate();

    assertThat(upgradeResult.success).isTrue();
    assertThat(upgradeResult.migrationsExecuted).isGreaterThan(0);
    assertThat(upgradeMigrations.info().current()).isNotNull();
    assertThat(upgradeMigrations.info().current().getVersion())
        .isGreaterThan(MigrationVersion.fromVersion("1"));
    assertThat(upgradeMigrations.info().pending()).isEmpty();

    DriverManager.getConnection(databaseUrl, "sa", "").close();
    Files.deleteIfExists(databaseFile);
    Files.deleteIfExists(Path.of(databaseFile + ".mv.db"));
    Files.deleteIfExists(Path.of(databaseFile + ".trace.db"));
  }

  private org.flywaydb.core.api.configuration.FluentConfiguration flywayFor(String databaseUrl) {
    return Flyway.configure()
        .locations("classpath:db/migration")
        .dataSource(databaseUrl, "sa", "")
        .cleanDisabled(true);
  }
}
