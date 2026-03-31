# Arthexis Java Scaffold

This repository provides a Spring-based scaffold to mirror the Arthexis architecture in Java.

## Stack Mapping

- **Runtime and modularity:** Spring Boot + Spring Modulith
- **Data and migrations:** Spring Data JPA/Hibernate + Flyway
- **OCPP transport:** WebSocket endpoint (`/ws/ocpp`) with typed message model + Redis-backed continuity store
- **Task orchestration:** RabbitMQ + `@Scheduled` and `@Async` hooks (Quartz starter included)
- **Security:** Spring Security + OAuth2 Resource Server baseline
- **Observability:** Actuator + Prometheus + OpenTelemetry starter + JSON logs
- **Ops/local topology:** Docker Compose for Postgres/Redis/RabbitMQ/Prometheus/Grafana
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

## Quick Start

```bash
docker compose up -d postgres redis rabbitmq
mvn spring-boot:run
```

Use `SPRING_PROFILES_ACTIVE=h2` for local in-memory mode.

Enable the built-in charge-point simulator when you want this app to emulate a station and connect to a CSMS:

```bash
ARTHEXIS_OCPP_SIMULATOR_ENABLED=true mvn spring-boot:run
```

By default, the simulator targets `ws://localhost:8080/ws/ocpp` and identifies as `sim-cp-001`.

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
