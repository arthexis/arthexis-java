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
- `com.arthexis.platform.security` – API security policy

## Quick Start

```bash
docker compose up -d postgres redis rabbitmq
mvn spring-boot:run
```

Use `SPRING_PROFILES_ACTIVE=h2` for local in-memory mode.

## Next Steps for Arthexis Parity

1. Replace baseline OCPP handler with a standards-compliant adapter (1.6/2.0.1/2.1).
2. Add dedicated module per business app (billing, users, connectors, telemetry, firmware, etc).
3. Add admin UI (Jmix/Vaadin) with module-scoped CRUD and command views.
4. Add WebAuthn4J and TOTP providers to harden operator/admin authentication.
