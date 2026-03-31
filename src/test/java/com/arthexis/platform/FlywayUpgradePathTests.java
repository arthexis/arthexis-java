package com.arthexis.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
      try (PreparedStatement insert =
          connection.prepareStatement(
              """
              insert into charging_station (station_id, status, last_seen_at)
              values (?, ?, CURRENT_TIMESTAMP)
              """)) {
        insert.setString(1, "CP-LEGACY-V1");
        insert.setString(2, "ONLINE");
        insert.executeUpdate();
      }
    }

    Flyway upgradeMigrations = flywayFor(databaseUrl).load();
    MigrateResult upgradeResult = upgradeMigrations.migrate();

    assertThat(upgradeResult.success).isTrue();
    assertThat(upgradeResult.migrationsExecuted).isGreaterThan(0);
    assertThat(upgradeMigrations.info().current()).isNotNull();
    assertThat(upgradeMigrations.info().current().getVersion())
        .isGreaterThan(MigrationVersion.fromVersion("1"));
    assertThat(upgradeMigrations.info().pending()).isEmpty();

    try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
      try (PreparedStatement query =
          connection.prepareStatement(
              "select status, enabled, created_at from charging_station where station_id = ?")) {
        query.setString(1, "CP-LEGACY-V1");
        try (ResultSet resultSet = query.executeQuery()) {
          assertThat(resultSet.next()).isTrue();
          assertThat(resultSet.getString("status")).isEqualTo("ONLINE");
          assertThat(resultSet.getBoolean("enabled")).isTrue();
          assertThat(resultSet.getTimestamp("created_at")).isNotNull();
        }
      }
    }

    cleanup(databaseFile);
  }

  @Test
  void migratesExistingInstallFromV2ToLatestWithoutTelemetryDataLoss() throws Exception {
    Path databaseFile = Files.createTempFile("arthexis-upgrade-v2", ".db");
    String databaseUrl =
        "jdbc:h2:file:"
            + databaseFile.toAbsolutePath()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

    Flyway initialInstallMigrations =
        flywayFor(databaseUrl).target(MigrationVersion.fromVersion("2")).load();
    MigrateResult initialInstallResult = initialInstallMigrations.migrate();

    assertThat(initialInstallResult.success).isTrue();
    assertThat(initialInstallMigrations.info().current()).isNotNull();
    assertThat(initialInstallMigrations.info().current().getVersion().getVersion()).isEqualTo("2");

    try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
      try (PreparedStatement insert =
          connection.prepareStatement(
              """
              insert into telemetry_sample (station_id, metric_name, metric_value, sampled_at)
              values (?, ?, ?, CURRENT_TIMESTAMP)
              """)) {
        insert.setString(1, "CP-LEGACY-V2");
        insert.setString(2, "Power.Active.Import");
        insert.setDouble(3, 7.25d);
        insert.executeUpdate();
      }
    }

    Flyway upgradeMigrations = flywayFor(databaseUrl).load();
    MigrateResult upgradeResult = upgradeMigrations.migrate();

    assertThat(upgradeResult.success).isTrue();
    assertThat(upgradeResult.migrationsExecuted).isGreaterThan(0);

    try (Connection connection = DriverManager.getConnection(databaseUrl, "sa", "")) {
      try (PreparedStatement query =
          connection.prepareStatement(
              """
              select metric_name, metric_value, scope_type, scope_identifier
              from telemetry_sample where station_id = ?
              """)) {
        query.setString(1, "CP-LEGACY-V2");
        try (ResultSet resultSet = query.executeQuery()) {
          assertThat(resultSet.next()).isTrue();
          assertThat(resultSet.getString("metric_name")).isEqualTo("Power.Active.Import");
          assertThat(resultSet.getDouble("metric_value")).isEqualTo(7.25d);
          assertThat(resultSet.getString("scope_type")).isNull();
          assertThat(resultSet.getString("scope_identifier")).isNull();
        }
      }
    }

    cleanup(databaseFile);
  }

  private org.flywaydb.core.api.configuration.FluentConfiguration flywayFor(String databaseUrl) {
    return Flyway.configure()
        .locations("classpath:db/migration")
        .dataSource(databaseUrl, "sa", "")
        .cleanDisabled(true);
  }

  private void cleanup(Path databaseFile) throws Exception {
    Files.deleteIfExists(databaseFile);
    Files.deleteIfExists(Path.of(databaseFile + ".mv.db"));
    Files.deleteIfExists(Path.of(databaseFile + ".trace.db"));
  }
}
