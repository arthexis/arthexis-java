# OCPP Payload Normalization Compatibility Spec

This document captures the payload variants currently accepted by `com.arthexis.platform.ocpp.OcaOcppPayloadNormalizer` and `OcaOcppBridgeService` action handling.

## Scope

Supported incoming actions:

- `BootNotification`
- `Heartbeat`
- `StatusNotification`
- `MeterValues`
- `TransactionEvent`
- `Authorize`
- `DiagnosticsStatusNotification`
- `FirmwareStatusNotification`
- `AvailabilityStatusNotification`

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

### Authorize

| Variant | Required fields accepted | Optional fields accepted | Notes |
|---|---|---|---|
| 1.6J | none for parser entry; identity token field recommended | `stationId`, `idTag`, `certificateStatus` | Returns normalized identity token hints without changing metering behavior. |
| 2.x compatibility | none | `chargingStation.serialNumber`, `idToken`, `certificateStatus` | `idToken` and legacy `idTag` are both accepted. |

Normalizer output contract for `Authorize`:

- Always includes `stationId` as string.
- Includes `idToken` and/or `idTag` when present.
- Includes `certificateStatus` when present.

### DiagnosticsStatusNotification

| Variant | Required fields accepted | Optional fields accepted | Notes |
|---|---|---|---|
| 1.6J | none | `stationId`, `status` | Status is captured for bridge-side lifecycle tracking. |
| 2.x compatibility | none | `chargingStation.serialNumber`, `uploadStatus`, `diagnosticsStatus` | Fallback order is `status` → `uploadStatus` → `diagnosticsStatus`. |

Normalizer output contract for diagnostics status:

- Always includes `stationId`.
- Includes normalized `status` when any accepted status key is present.

### FirmwareStatusNotification

| Variant | Required fields accepted | Optional fields accepted | Notes |
|---|---|---|---|
| 1.6J | none | `stationId`, `status` | Legacy field names remain valid. |
| 2.x | none | `chargingStation.serialNumber`, `firmwareStatus`, `updateStatus`, `requestId` | Fallback order is `status` → `firmwareStatus` → `updateStatus`. |

Normalizer output contract for firmware status:

- Always includes `stationId`.
- Includes normalized `status` and optional `requestId` when present.

### AvailabilityStatusNotification

| Variant | Required fields accepted | Optional fields accepted | Notes |
|---|---|---|---|
| 1.6J compatibility | none | `stationId`, `status`, `connectorId` | Per-connector lifecycle state supported via connector identifiers. |
| 2.x | none | `chargingStation.serialNumber`, `operationalStatus`, `availabilityType`, `evse.id`, `evse.connectorId` | Fallback status order is `status` → `operationalStatus` → `availabilityType`. |

Normalizer output contract for availability status:

- Always includes `stationId`.
- Includes normalized `status` when available.
- Includes string `evseId` / `connectorId` when present.

## Command dispatch capability registry

Outgoing action support is profile-driven via `arthexis.ocpp.commands.profile-capabilities`.

Default registry:

- `python-ocpp16`: `RemoteStartTransaction`, `RemoteStopTransaction`, `RequestStartTransaction`, `RequestStopTransaction`, `SetChargingProfile`, `ChangeAvailability`, `Reset`, `GetDiagnostics`, `UpdateFirmware`.
- `python-ocpp2x`: `RequestStartTransaction`, `RequestStopTransaction`, `RemoteStartTransaction`, `RemoteStopTransaction`, `SetChargingProfile`, `Reset`, `ChangeAvailability`, `GetDiagnostics`, `UpdateFirmware`.

Example override in `application.yml` (no code change required):

```yaml
arthexis:
  ocpp:
    commands:
      profile-capabilities:
        python-ocpp16:
          - RemoteStartTransaction
          - RemoteStopTransaction
          - RequestStartTransaction
          - RequestStopTransaction
          - SetChargingProfile
          - ChangeAvailability
          - Reset
          - GetDiagnostics
          - UpdateFirmware
        python-ocpp2x:
          - RequestStartTransaction
          - RequestStopTransaction
          - RemoteStartTransaction
          - RemoteStopTransaction
          - SetChargingProfile
          - Reset
          - ChangeAvailability
          - GetDiagnostics
          - UpdateFirmware
        vendor-profile-x:
          - TriggerMessage
          - UnlockConnector
```

Behavior note: configured profile entries are merged with defaults. Custom profile keys are added, and default
`python-ocpp16` / `python-ocpp2x` entries remain available unless explicitly overridden by key.

Fallback rules:

1. Exact profile key match (case-insensitive) is used when configured.
2. Unknown profiles containing `"2"` fall back to `python-ocpp2x`.
3. All other unknown profiles fall back to `python-ocpp16`.

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
- `authorize.ocpp16.json`
- `diagnostics_status.ocpp16.json`
- `firmware_status.ocpp2x.json`
- `availability_status.ocpp2x.json`

These fixtures are the contract-test inputs for backward compatibility assertions in `OcaOcppPayloadNormalizerContractTests`.
