# OCPP Payload Normalization Compatibility Spec

This document captures the payload variants currently accepted by `com.arthexis.platform.ocpp.OcaOcppPayloadNormalizer` and `OcaOcppBridgeService` action handling.

## Scope

Supported incoming actions:

- `BootNotification`
- `Heartbeat`
- `StatusNotification`
- `MeterValues`
- `TransactionEvent`

Supported protocol families:

- **OCPP 1.6J** style payloads (`stationId`, `meterValue`).
- **OCPP 2.x** style payloads (`chargingStation.serialNumber`, `meterValues`).

## Station identity resolution contract

`resolveStationId(sessionId, payload)` resolves station identity in this strict order:

1. `payload.stationId` when non-blank.
2. `payload.chargingStation.serialNumber` when non-blank.
3. `payload.chargingStation.model` when non-blank.
4. `sessionId` fallback.

Edge-case fixtures are included for each identity path, including explicit `sessionId` fallback.

## Action compatibility matrix

### BootNotification

| Variant | Required fields accepted | Optional fields accepted | Notes |
|---|---|---|---|
| 1.6J | none (for normalizer), `stationId` recommended | `stationId` | Identity may resolve from `stationId`; otherwise from session fallback chain. |
| 2.x | none (for normalizer), `chargingStation.serialNumber` recommended | `chargingStation.serialNumber`, `chargingStation.model` | Identity may resolve from charging station block. |

### Heartbeat

| Variant | Required fields accepted | Optional fields accepted | Notes |
|---|---|---|---|
| 1.6J | none | `stationId` | Empty payload accepted; session fallback supported. |
| 2.x | none | `chargingStation.serialNumber`, `chargingStation.model` | Charging station block can establish identity. |

### StatusNotification

| Variant | Required fields accepted | Optional fields accepted | Notes |
|---|---|---|---|
| 1.6J | none for identity; status uses `status` or fallback | `stationId`, `status` | `status` consumed by bridge status update; normalizer identity remains backward compatible. |
| 2.x | none for identity; status uses `connectorStatus` fallback | `chargingStation.serialNumber`, `chargingStation.model`, `connectorStatus` | Session fallback maintained when no identity fields are provided. |

### MeterValues

| Variant | Required fields accepted | Optional fields accepted | Notes |
|---|---|---|---|
| 1.6J | `meterValue[]` recommended for metric extraction | `stationId`, `meterValue[].timestamp`, `meterValue[].sampledValue[].measurand`, `meterValue[].sampledValue[].value` | Numeric strings and numbers both accepted; non-numeric values ignored. |
| 2.x | `meterValues[]` recommended for metric extraction | `chargingStation.serialNumber`, `meterValues[].timestamp`, `meterValues[].sampledValue[].*` | `meterValues` alias is supported when `meterValue` absent. |

Normalizer output contract for `MeterValues`:

- Always includes `stationId` as string.
- Includes `sampledAt` from first timestamp if present, otherwise current ISO-8601 instant.
- Adds metrics as numeric (`Double`) keyed by measurand.

### TransactionEvent

| Variant | Required fields accepted | Optional fields accepted | Notes |
|---|---|---|---|
| 1.6J compatibility payloads | none (normalizer still accepts `stationId` and 1.6-like meter layout) | `stationId`, `timestamp`, `totalCost`, `meterValue[]`, `meterValues[]` | Backward-compatible handling for mixed payloads from legacy clients/tests. |
| 2.x | none for parser entry; `timestamp` and meter arrays recommended | `chargingStation.serialNumber`, `timestamp`, `eventType`, `totalCost`, `meterValue[]`, `meterValues[]` | Event type is consumed by bridge for status transitions; normalizer focuses on telemetry flattening. |

Normalizer output contract for `TransactionEvent`:

- Always includes `stationId` as string.
- Includes `sampledAt` from top-level `timestamp` when present, otherwise first meter timestamp or current instant.
- Includes `totalCost` when numeric.
- Adds measurand metrics as numeric (`Double`).

## Canonical fixtures

Canonical fixture directory: [`docs/ocpp/fixtures`](./fixtures)

Included fixture files:

- `boot_notification.ocpp16.json`
- `boot_notification.ocpp2x.json`
- `heartbeat.ocpp16.json`
- `heartbeat.ocpp2x.json`
- `status_notification.ocpp16.json`
- `status_notification.ocpp2x.json`
- `meter_values.ocpp16.json`
- `meter_values.ocpp2x.json`
- `transaction_event.ocpp16.json`
- `transaction_event.ocpp2x.json`

These fixtures are the contract-test inputs for backward compatibility assertions in `OcaOcppPayloadNormalizerContractTests`.
