# Arthexis Java Scaffold

This repository provides a Spring-based scaffold to mirror the Arthexis architecture in Java.

## Stack Mapping

- **Runtime and modularity:** Spring Boot + Spring Modulith
- **Data and migrations:** Spring Data JPA/Hibernate + Flyway
- **OCPP transport:** WebSocket endpoint (`/ws/ocpp`) with typed message model + Redis-backed continuity store
- **Task orchestration:** RabbitMQ + `@Scheduled` and `@Async` hooks (Quartz starter included)
- **Security:** Spring Security + OAuth2 Resource Server baseline
- **Observability:** Actuator + Prometheus + OpenTelemetry starter + JSON logs
- **Ops/local topology:** systemd-first services for Postgres/Redis/RabbitMQ, with optional Docker Compose
- **Quality and tests:** JUnit 5, Spring Modulith tests, ArchUnit, Testcontainers dependencies

## Module Layout

- `com.arthexis.platform.app` – app/module registry and composition root
- `com.arthexis.platform.ocpp` – OCPP websocket and continuity primitives
- `com.arthexis.platform.charging` – charging station domain model
- `com.arthexis.platform.operations` – async/scheduled orchestration
- `com.arthexis.platform.telemetry` – telemetry ingestion and persistence primitives
- `com.arthexis.platform.auth` – RFID authorization, account login flows, and charge-point QR login sessions
- `com.arthexis.platform.security` – API security policy
- `com.arthexis.platform.simulator` – OCPP charge-point simulator for local CSMS flows
- `com.arthexis.platform.transfer` – OCPP-aware FTP endpoint with charger-bound credentials for diagnostics/firmware artifacts


## Supported Build Targets

This scaffold now has explicit build-target support for **Raspberry Pi 4 Model B (ARM64)** using:

- **Debian 12 (Bookworm)**
- **Ubuntu 22.04 LTS (Jammy)**
- **Ubuntu 24.04 LTS (Noble)**

The CI pipeline executes ARM64 compatibility checks in both Debian and Ubuntu container images to validate Arthexis Java build/test paths for Raspberry Pi-class environments.

## Arthexis-style CLI Surfaces

To align local workflows with the Arthexis suite command model, this scaffold now includes a small wrapper CLI:

```bash
./bin/arthexis help
./bin/arthexis install
./bin/arthexis upgrade
```

- `install` runs local dependency bootstrapping (systemd by default, `--docker` optional) and full verification.
- `install` accepts `--service '<prefix-%s>'` to resolve systemd service names in host-specific deployments.
  If `%s` is omitted, `-%s` is appended automatically.
- `upgrade` runs the Flyway upgrade-path verification test used by CI.
- `verify` mirrors CI install modes (`new-install`, `upgrade-install`).

## Quick Start

```bash
./bin/arthexis install
```

Optional Docker-backed quick start:

```bash
./bin/arthexis install --docker
```

Use `SPRING_PROFILES_ACTIVE=h2` for local in-memory mode.

Enable the built-in charge-point simulator when you want this app to emulate a station and connect to a CSMS:

```bash
ARTHEXIS_OCPP_SIMULATOR_ENABLED=true mvn spring-boot:run
```

By default, the simulator targets `ws://localhost:8080/ws/ocpp` and identifies as `sim-cp-001`.

You can enable the Arthexis-style OCPP FTP surface to expose charger-scoped firmware/diagnostics artifacts:

```yaml
arthexis:
  ocpp:
    ftp:
      enabled: true
      bind-address: 0.0.0.0
      public-host: ftp.arthexis.local
      port: 2121
      root-directory: ./var/ocpp-ftp
      bindings:
        - id: fleet-alpha
          username: fleet-alpha
          password: ${FTP_ALPHA_PASSWORD}
          charger-ids: [cp-001, cp-002]
```

If you want JWT/OAuth2 resource-server validation enabled, set:

```bash
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8081/realms/arthexis
```

OpenTelemetry SDK autoconfiguration is disabled by default in this scaffold to keep local startup self-contained.
Enable it when your telemetry stack is available:

```bash
OTEL_SDK_DISABLED=false
```

## Next Steps for Arthexis Parity

1. Extend OCA-OCPP coverage beyond the current 1.6J + 2.x baseline (BootNotification, Heartbeat, StatusNotification, MeterValues, TransactionEvent).
2. Continue adding dedicated business modules (billing, users, connectors, firmware, etc).
3. Add admin UI (Jmix/Vaadin) with module-scoped CRUD and command views.
4. Add WebAuthn4J and TOTP providers to harden operator/admin authentication.
