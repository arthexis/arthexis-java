# Install Guide

This guide explains how to install and run the Arthexis Java scaffold locally. It now includes Arthexis-style CLI command surfaces for install and upgrade workflows.

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


### Raspberry Pi 4B (Debian/Ubuntu) build targets

Arthexis Java is supported on **Raspberry Pi 4 Model B (64-bit OS)** for:

- **Debian 12 (Bookworm, arm64)**
- **Ubuntu 22.04 LTS (Jammy, arm64)**
- **Ubuntu 24.04 LTS (Noble, arm64)**

Recommended minimums on Pi:

- 4 GB RAM (8 GB preferred)
- 64-bit Java 21 runtime
- Maven 3.9+

Install Java on Debian/Ubuntu ARM64:

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk
java -version
```

Install **Maven 3.9+** from the official Apache binaries (APT versions on these distros can be older than 3.9):

```bash
MAVEN_VERSION=3.9.11
curl -fsSL https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz -o /tmp/apache-maven.tar.gz
sudo tar -xzf /tmp/apache-maven.tar.gz -C /opt
sudo ln -sfn /opt/apache-maven-${MAVEN_VERSION} /opt/maven
echo 'export PATH=/opt/maven/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
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

## 3) Use the Arthexis-style CLI surfaces

The repository includes `./bin/arthexis` with command surfaces aligned to Arthexis workflows:

```bash
./bin/arthexis help
./bin/arthexis install
./bin/arthexis upgrade
```

- `install` starts Docker dependencies, runs full `mvn -B verify`, then starts the app.
- `upgrade` executes Flyway upgrade validation (`FlywayUpgradePathTests`) like CI.
- `verify` accepts `new-install` or `upgrade-install`.

Optional flags:

```bash
./bin/arthexis install --with-observability
./bin/arthexis run --h2
```

## 4) Start infrastructure services (Postgres, Redis, RabbitMQ)

From the repository root:

```bash
docker compose up -d postgres redis rabbitmq
```

Optional observability stack (Prometheus + Grafana):

```bash
docker compose up -d prometheus grafana
```

## 5) Run the application

```bash
mvn spring-boot:run
```

The app starts with the default Spring profile and connects to the Docker-backed services.

## 6) Alternative local mode: in-memory H2

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

## 7) Optional security configuration (JWT issuer)

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

## 8) Optional telemetry configuration (OpenTelemetry)

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

## 9) Verify the install

Run the test suite:

```bash
mvn test
```

If tests pass and the app starts cleanly, your local install is complete.
