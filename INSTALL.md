# Install Guide

This guide explains how to install and run the Arthexis Java scaffold locally.

## 1) Prerequisites

Install the following tools first:

- **Git**
- **Java 21** (required by this project)
- **Maven 3.9+**
- **Docker + Docker Compose plugin** (recommended for Postgres/Redis/RabbitMQ)

### Windows setup notes

- Install **Git for Windows** and use **PowerShell** or **Git Bash**.
- Install **Temurin/OpenJDK 21** and verify with `java -version`.
- Install **Maven 3.9+** and verify with `mvn -v`.
- Install **Docker Desktop** and ensure Docker Compose v2 is available (`docker compose version`).


## Raspberry Pi 4B (Debian/Ubuntu) build targets

Arthexis Java is supported on **Raspberry Pi 4 Model B (64-bit OS)** for both:

- **Debian 12 (Bookworm, arm64)**
- **Ubuntu 22.04 LTS (Jammy, arm64)**
- **Ubuntu 24.04 LTS (Noble, arm64)**

Recommended minimums on Pi:

- 4 GB RAM (8 GB preferred)
- 64-bit Java 21 runtime
- Maven 3.9+

Install Java/Maven on Debian/Ubuntu ARM64:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk maven
java -version
mvn -v
```

Then run the same Arthexis commands as other Linux hosts:

```bash
mvn -B test
mvn -B spring-boot:run
```

## 2) Clone the repository

```bash
git clone <your-fork-or-repo-url>
cd arthexis-java
```

## 3) Start infrastructure services (Postgres, Redis, RabbitMQ)

From the repository root:

```bash
docker compose up -d postgres redis rabbitmq
```

Optional observability stack (Prometheus + Grafana):

```bash
docker compose up -d prometheus grafana
```

## 4) Run the application

```bash
mvn spring-boot:run
```

The app starts with the default Spring profile and connects to the Docker-backed services.

## 5) Alternative local mode: in-memory H2

If you want to run without Postgres, enable the `h2` profile:

### Bash / Zsh / Git Bash

```bash
export SPRING_PROFILES_ACTIVE=h2
mvn spring-boot:run
```

### PowerShell

```powershell
$env:SPRING_PROFILES_ACTIVE = "h2"
mvn spring-boot:run
```

## 6) Optional security configuration (JWT issuer)

If you want OAuth2 resource-server validation enabled, set:

### Bash / Zsh / Git Bash

```bash
export SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=http://localhost:8081/realms/arthexis
```

### PowerShell

```powershell
$env:SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI = "http://localhost:8081/realms/arthexis"
```

Then run the app as usual.

## 7) Optional telemetry configuration (OpenTelemetry)

OpenTelemetry SDK autoconfiguration is disabled by default for local convenience.
Enable it when your telemetry backend is available:

### Bash / Zsh / Git Bash

```bash
export OTEL_SDK_DISABLED=false
```

### PowerShell

```powershell
$env:OTEL_SDK_DISABLED = "false"
```

## 8) Verify the install

Run the test suite:

```bash
mvn test
```

If tests pass and the app starts cleanly, your local install is complete.
